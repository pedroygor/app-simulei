package br.com.simulados.api.shared.security;

import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Existindo este bean, o Boot para de criar o usuário em memória com senha no log
// e o AuthenticationManager passa a autenticar contra o nosso banco
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioDetails::new)
                // O Spring converte em BadCredentials genérico: quem loga não descobre
                // se o e-mail existe ou se a senha está errada
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
