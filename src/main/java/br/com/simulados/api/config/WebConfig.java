package br.com.simulados.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
// VIA_DTO: serializa Page num JSON estável ({content, page{...}}) em vez do
// PageImpl interno, cuja estrutura o Spring não garante entre versões
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}
