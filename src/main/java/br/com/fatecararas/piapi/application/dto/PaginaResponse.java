package br.com.fatecararas.piapi.application.dto;

import java.util.List;

// Envelope da listagem: conteúdo e metadados de paginação; T é o tipo de cada item.
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
}
