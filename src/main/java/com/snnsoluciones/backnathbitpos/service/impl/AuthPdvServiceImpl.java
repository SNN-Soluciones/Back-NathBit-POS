package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPinRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvResponse;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.AsistenciaService;
import com.snnsoluciones.backnathbitpos.service.AuthPdvService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Implementación del servicio de autenticación con PIN en PDV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPdvServiceImpl implements AuthPdvService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DispositivoRepository dispositivoRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public LoginPdvResponse loginConPin(String deviceToken, LoginPdvRequest request) {
        log.info("Login PDV - Usuario: {}", request.getUsuarioId());

        // 1. Validar dispositivo
        Dispositivo dispositivo = dispositivoRepository.findByTokenAndActivoTrueWithTenant(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado o inactivo"));

        // 2. Obtener usuario del schema del tenant via JDBC
        String schemaName = dispositivo.getTenant().getSchemaName();
        String sql = String.format("""
        SELECT id, nombre, apellidos, email, rol, pin, pin_longitud, 
               requiere_cambio_pin, activo
        FROM %s.usuarios WHERE id = ?
        """, schemaName);

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(sql, request.getUsuarioId());
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        // 3. Validar activo
        if (!Boolean.TRUE.equals(row.get("activo"))) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        // 4. Validar PIN configurado
        String pinHash = (String) row.get("pin");
        if (pinHash == null || pinHash.isEmpty()) {
            throw new BadRequestException("El usuario no tiene PIN configurado");
        }

        // 5. Validar PIN
        if (!passwordEncoder.matches(request.getPin(), pinHash)) {
            log.warn("PIN incorrecto para usuario: {}", request.getUsuarioId());
            throw new UnauthorizedException("PIN incorrecto");
        }

        // 6. Registrar uso del dispositivo
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);

        // 7. Generar JWT
        String token = jwtTokenProvider.generateTokenWithContext(
            (Long) row.get("id"),
            (String) row.get("email"),
            (String) row.get("rol"),
            dispositivo.getTenant().getEmpresaLegacyId(),
            dispositivo.getSucursalId()
        );

        boolean requiereCambioPin = Boolean.TRUE.equals(row.get("requiere_cambio_pin"));

        log.info("Login exitoso - Usuario: {}, Tenant: {}",
            request.getUsuarioId(), dispositivo.getTenant().getCodigo());

        return LoginPdvResponse.builder()
            .token(token)
            .usuario(LoginPdvResponse.UsuarioInfo.builder()
                .id((Long) row.get("id"))
                .nombre((String) row.get("nombre"))
                .apellidos((String) row.get("apellidos"))
                .nombreCompleto(row.get("nombre") + " " +
                    (row.get("apellidos") != null ? row.get("apellidos") : ""))
                .email((String) row.get("email"))
                .rol((String) row.get("rol"))
                .build())
            .empresa(LoginPdvResponse.EmpresaInfo.builder()
                .id(dispositivo.getTenant().getEmpresaLegacyId())
                .nombreComercial(dispositivo.getTenant().getNombre())
                .build())
            .sucursal(LoginPdvResponse.SucursalInfo.builder()
                .id(dispositivo.getSucursalId())
                .nombre(dispositivo.getSucursalNombre())
                .build())
            .requiereCambioPin(requiereCambioPin)
            .build();
    }

    @Override
    @Transactional
    public void cambiarPin(Long usuarioId, CambiarPinRequest request) {
        log.info("Cambiando PIN - Usuario: {}", usuarioId);

        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 2. Validar que nuevo PIN y confirmación coinciden
        if (!request.getNuevoPin().equals(request.getConfirmarPin())) {
            throw new BadRequestException("El nuevo PIN y su confirmación no coinciden");
        }

        // 3. Si ya tiene PIN, validar el PIN actual
        if (usuario.getPin() != null && !usuario.getPin().isEmpty()) {
            if (request.getPinActual() == null || request.getPinActual().isEmpty()) {
                throw new BadRequestException("Debe proporcionar el PIN actual");
            }

            if (!passwordEncoder.matches(request.getPinActual(), usuario.getPin())) {
                throw new UnauthorizedException("PIN actual incorrecto");
            }
        }

        // 4. Validar que el nuevo PIN es diferente al actual
        if (usuario.getPin() != null && passwordEncoder.matches(request.getNuevoPin(), usuario.getPin())) {
            throw new BadRequestException("El nuevo PIN debe ser diferente al actual");
        }

        // 5. Hashear y guardar nuevo PIN
        usuario.setPin(passwordEncoder.encode(request.getNuevoPin()));
        usuario.setPinLongitud(request.getNuevoPin().length());
        usuario.setRequiereCambioPin(false); // Marca que ya no requiere cambio

        usuarioRepository.save(usuario);

        log.info("PIN cambiado exitosamente - Usuario: {}", usuarioId);
    }

    @Override
    @Transactional
    public String generarPinAleatorio(Long usuarioId) {
        log.info("Generando PIN aleatorio - Usuario: {}", usuarioId);

        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 2. Generar PIN aleatorio de 4 dígitos
        String pinAleatorio = String.format("%04d", RANDOM.nextInt(10000));

        // 3. Hashear y guardar
        usuario.setPin(passwordEncoder.encode(pinAleatorio));
        usuario.setPinLongitud(4);
        usuario.setRequiereCambioPin(true); // Usuario debe cambiar el PIN

        usuarioRepository.save(usuario);

        log.info("PIN aleatorio generado - Usuario: {}", usuarioId);

        return pinAleatorio; // Retornar sin hashear para mostrarlo al admin
    }

    @Override
    @Transactional
    public String resetearPin(Long usuarioId) {
        log.info("Reseteando PIN - Usuario: {}", usuarioId);

        // Resetear es lo mismo que generar uno nuevo
        return generarPinAleatorio(usuarioId);
    }
}