package br.com.simulados.api.usuario.api;

import br.com.simulados.api.usuario.application.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        var usuario = usuarioService.criar(request.nome(), request.email(), request.senha());

        // 201 com Location apontando pro recurso criado, mesmo que o GET /usuarios/{id}
        // ainda não exista — é o contrato REST correto desde já
        var location = uriBuilder.path("/usuarios/{id}").buildAndExpand(usuario.getId()).toUri();
        return ResponseEntity.created(location).body(usuarioMapper.toResponse(usuario));
    }

    // O resource server injeta o token validado como principal; o sub é o id
    // do usuário — nunca confiamos em id vindo de parâmetro pra dizer "quem sou eu"
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        var usuario = usuarioService.buscarPorId(UUID.fromString(jwt.getSubject()));
        return usuarioMapper.toResponse(usuario);
    }
}
