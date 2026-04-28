package com.smartcity.backend.controller;

import com.smartcity.backend.entity.Service;
import com.smartcity.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;

    @GetMapping
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @PostMapping
    public Service createService(@RequestBody Service service) {
        return serviceRepository.save(service);
    }

    @PutMapping("/{id}")
    public Service updateService(@PathVariable Long id, @RequestBody Service service) {
        Service existingService = serviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found"));
        existingService.setName(service.getName());
        existingService.setCategory(service.getCategory());
        existingService.setLocation(service.getLocation());
        existingService.setHours(service.getHours());
        existingService.setPhone(service.getPhone());
        existingService.setStatus(service.getStatus());
        existingService.setDescription(service.getDescription());
        return serviceRepository.save(existingService);
    }

    @DeleteMapping("/{id}")
    public void deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
    }
}