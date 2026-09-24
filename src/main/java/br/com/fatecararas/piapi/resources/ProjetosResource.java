package br.com.fatecararas.piapi.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projetos")
public class ProjetosResource {
    
    @PostMapping
    public ResponseEntity<Void> criarProjeto() {
        System.out.println("Requisicao para criacao de projeto");
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Void> buscarProjeto() {
        System.out.println("Requisicao para consulta de projeto");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> excluirProjeto() {
        System.out.println("Requisicao para exclusão de projeto");
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<Void> atualizarProjeto() {
        System.out.println("Requisicao para atualização de projeto");
        return ResponseEntity.ok().build();
    }
}
