package io.github.asmitmans.iotbackend.controller;

import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeRequest;
import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeResponse;
import io.github.asmitmans.iotbackend.service.CommandTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Command Types", description = "Global catalog of device command types")
@RestController
@RequestMapping("/api/v1/command-types")
public class CommandTypeController {

  private final CommandTypeService commandTypeService;

  public CommandTypeController(CommandTypeService commandTypeService) {
    this.commandTypeService = commandTypeService;
  }

  @Operation(summary = "List all command types", security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<List<CommandTypeResponse>> getAll() {
    return ResponseEntity.ok(commandTypeService.getAll());
  }

  @Operation(summary = "Get command type by ID", security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommandTypeResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(commandTypeService.getById(id));
  }

  @Operation(summary = "Create command type", security = @SecurityRequirement(name = "bearerAuth"))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CommandTypeResponse> create(@RequestBody @Valid CommandTypeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(commandTypeService.create(request));
  }

  @Operation(summary = "Update command type", security = @SecurityRequirement(name = "bearerAuth"))
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CommandTypeResponse> update(@PathVariable Long id, @RequestBody @Valid CommandTypeRequest request) {
    return ResponseEntity.ok(commandTypeService.update(id, request));
  }

  @Operation(summary = "Delete command type", security = @SecurityRequirement(name = "bearerAuth"))
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    commandTypeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}