package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPinRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvResponse;
import com.snnsoluciones.backnathbitpos.entity.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.AuthPdvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final DispositivoRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public LoginPdvResponse loginConPin(String deviceToken, LoginPdvRequest request) {
        log.info("Login PDV - Usuario: {}, Dispositivo: {}", request.getUsuarioId(), deviceToken);

        // 1. Validar dispositivo
        Dispositivo dispositivo = dispositivoRepository.findByDeviceTokenAndActivoTrue(deviceToken)
            .orElseThrow(() -> new UnauthorizedException("Dispositivo no autorizado o inactivo"));

        // 2. Buscar usuario
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Validar que el usuario está activo
        if (!usuario.getActivo()) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        // 4. Validar que el usuario tiene PIN configurado
        if (usuario.getPin() == null || usuario.getPin().isEmpty()) {
            throw new BadRequestException("El usuario no tiene PIN configurado");
        }

        // 5. Validar que el usuario pertenece a la empresa del dispositivo
        boolean perteneceAEmpresa = usuario.getUsuarioEmpresas().stream()
            .anyMatch(ue -> ue.getEmpresa().getId().equals(dispositivo.getEmpresa().getId()));

        if (!perteneceAEmpresa && !usuario.esRolSistema()) {
            throw new UnauthorizedException("El usuario no pertenece a esta empresa");
        }

        // 6. Validar PIN
        if (!passwordEncoder.matches(request.getPin(), usuario.getPin())) {
            log.warn("PIN incorrecto para usuario: {}", usuario.getId());
            throw new UnauthorizedException("PIN incorrecto");
        }

        // 7. Actualizar último uso del dispositivo
        dispositivo.registrarUso();
        dispositivoRepository.save(dispositivo);

        // 8. Generar JWT con contexto de empresa y sucursal
        String token = jwtTokenProvider.generateTokenWithContext(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name(),
            dispositivo.getEmpresa().getId(),
            dispositivo.getSucursal().getId()
        );

        // 9. Determinar ruta de destino según rol
        String rutaDestino = determinarRutaDestino(usuario, dispositivo);

        log.info("Login exitoso - Usuario: {}, Rol: {}", usuario.getId(), usuario.getRol());

        // 10. Construir response
        return LoginPdvResponse.builder()
            .token(token)
            .usuario(LoginPdvResponse.UsuarioInfo.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .nombreCompleto(usuario.getNombre() + " " + (usuario.getApellidos() != null ? usuario.getApellidos() : ""))
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build())
            .empresa(LoginPdvResponse.EmpresaInfo.builder()
                .id(dispositivo.getEmpresa().getId())
                .nombreComercial(dispositivo.getEmpresa().getNombreComercial())
                .build())
            .sucursal(LoginPdvResponse.SucursalInfo.builder()
                .id(dispositivo.getSucursal().getId())
                .nombre(dispositivo.getSucursal().getNombre())
                .build())
            .requiereCambioPin(usuario.getRequiereCambioPin())
            .rutaDestino(rutaDestino)
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
        usuario.setRequiereCambioPassword(false); // Marca que ya no requiere cambio

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

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Determina la ruta de destino según el rol del usuario
     */
    private String determinarRutaDestino(Usuario usuario, Dispositivo dispositivo) {
        RolNombre rol = usuario.getRol();

        // SUPER_ADMIN y ADMIN van al dashboard de su empresa
        if (rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN) {
            return "/dashboard-admin-empresa/" + dispositivo.getEmpresa().getId();
        }

        // ROOT y SOPORTE van al dashboard global
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            return "/dashboard-admin";
        }

        // Roles operativos (CAJERO, MESERO, etc.) van al POS
        return "/pos";
    }
}