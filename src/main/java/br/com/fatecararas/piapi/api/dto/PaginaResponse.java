package br.com.fatecararas.piapi.api.dto;

import java.util.List;

public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
}
