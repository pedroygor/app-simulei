package br.com.simulados.api.usuario.api;

import br.com.simulados.api.usuario.domain.Usuario;
import org.mapstruct.Mapper;

// componentModel = "spring": a implementação gerada vira um @Component
// injetável, em vez de exigir Mappers.getMapper(...)
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);
}
