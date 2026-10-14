package br.com.fatecararas.piapi.application.dto;

// Representação comum dos catálogos; campos sem uso naquele catálogo podem ser nulos.
public record ItemCatalogoResponse(Long id, String nome, String descricao, String sigla) {
}
