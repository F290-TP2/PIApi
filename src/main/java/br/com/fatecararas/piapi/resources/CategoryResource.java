package br.com.fatecararas.piapi.resources;

import br.com.fatecararas.piapi.domain.entity.CategoriaEntity;
import br.com.fatecararas.piapi.domain.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categorias")
public class CategoryResource {

    final CategoriaRepository repository;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CategoriaEntity createCategory(@RequestBody CategoriaEntity categoria) {
        return repository.save(categoria);
    }
}
