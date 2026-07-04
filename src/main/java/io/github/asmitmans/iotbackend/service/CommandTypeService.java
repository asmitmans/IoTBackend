package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeRequest;
import io.github.asmitmans.iotbackend.dto.commandtype.CommandTypeResponse;
import io.github.asmitmans.iotbackend.entity.CommandType;
import io.github.asmitmans.iotbackend.exception.ConflictException;
import io.github.asmitmans.iotbackend.exception.ResourceNotFoundException;
import io.github.asmitmans.iotbackend.repository.CommandTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommandTypeService {

    private final CommandTypeRepository commandTypeRepository;

    public CommandTypeService(CommandTypeRepository commandTypeRepository) {
        this.commandTypeRepository = commandTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<CommandTypeResponse> getAll() {
        return commandTypeRepository.findAll()
                                    .stream()
                                    .map(this::toResponse)
                                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommandTypeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CommandTypeResponse create(CommandTypeRequest request) {
        commandTypeRepository.findByName(request.getName())
                             .ifPresent(c -> {
                                 throw new ConflictException("CommandType with name '" + request.getName() + "' already exists");
                             });

        CommandType entity = new CommandType();
        applyRequest(entity, request);
        return toResponse(commandTypeRepository.save(entity));
    }

    @Transactional
    public CommandTypeResponse update(Long id, CommandTypeRequest request) {
        CommandType entity = findOrThrow(id);
        applyRequest(entity, request);
        return toResponse(commandTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        CommandType entity = findOrThrow(id);
        commandTypeRepository.delete(entity);
    }

    private void applyRequest(CommandType entity, CommandTypeRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
    }

    private CommandType findOrThrow(Long id) {
        return commandTypeRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("CommandType not found"));
    }

    private CommandTypeResponse toResponse(CommandType entity) {
        CommandTypeResponse r = new CommandTypeResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setDescription(entity.getDescription());
        return r;
    }
}