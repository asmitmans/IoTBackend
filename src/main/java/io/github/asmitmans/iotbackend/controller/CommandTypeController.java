package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeRequest;
import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeResponse;
import io.github.asmitmans.iotbackend.service.CommandTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/command-types")
public class CommandTypeController {

  private final CommandTypeService commandTypeService;

  public CommandTypeController(CommandTypeService commandTypeService) {
    this.commandTypeService = commandTypeService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<List<CommandTypeResponse>> getAll() {
    return ResponseEntity.ok(commandTypeService.getAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommandTypeResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(commandTypeService.getById(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CommandTypeResponse> create(
      @RequestBody @Valid CommandTypeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(commandTypeService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CommandTypeResponse> update(
      @PathVariable Long id, @RequestBody @Valid CommandTypeRequest request) {
    return ResponseEntity.ok(commandTypeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    commandTypeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
