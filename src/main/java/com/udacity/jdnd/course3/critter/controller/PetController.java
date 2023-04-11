package com.udacity.jdnd.course3.critter.controller;

import com.udacity.jdnd.course3.critter.request.PetRequest;
import com.udacity.jdnd.course3.critter.entity.Pet;
import com.udacity.jdnd.course3.critter.exceptions.MissingInfoException;
import com.udacity.jdnd.course3.critter.service.PetService;
import com.udacity.jdnd.course3.critter.service.UserService;
import com.udacity.jdnd.course3.critter.exceptions.CustomerNotFoundException;
import com.udacity.jdnd.course3.critter.exceptions.PetNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles web requests related to Pets.
 */
@RestController
@RequestMapping("/pet")
public class PetController {

    private static final String []  PROPERTIES_TO_IGNORE_ON_COPY = { "id" };

    @Autowired
    PetService petService;

    @Autowired
    UserService userService;

    @PostMapping("/{ownerId}")
    public PetRequest updatePet(@PathVariable(name="ownerId") Long ownerId, @RequestBody PetRequest petRequest){
        petRequest.setOwnerId(ownerId);
        return savePet(petRequest);
    }

    @PostMapping
    public PetRequest savePet(@RequestBody PetRequest petRequest) throws CustomerNotFoundException, MissingInfoException {
        // is the id null?
        long petId = Optional.ofNullable(petRequest.getId()).orElse(-1L);

        // get the pet if it exists
        Pet p = petService.getPet(Long.valueOf(petId)).orElseGet(Pet::new);

        // copy user input to the existing pet
        BeanUtils.copyProperties(petRequest, p, PROPERTIES_TO_IGNORE_ON_COPY);

        // save the pet to the owner.
        p = petService.save(p, petRequest.getOwnerId());

        // return the updated DTO
        PetRequest dto = new PetRequest();
        BeanUtils.copyProperties(p, dto);
        dto.setOwnerId(p.getOwner().getId());
        return dto;
    }

    @GetMapping("/{petId}")
    public PetRequest getPet(@PathVariable long petId) throws PetNotFoundException {
        PetRequest dto = new PetRequest();
        Pet p = petService.getPet(petId).orElseThrow(() -> new PetNotFoundException("ID: " + petId));
        BeanUtils.copyProperties(p, dto);
        dto.setOwnerId(p.getOwner().getId());
        return dto;
    }

    @GetMapping
    public List<PetRequest> getPets(){
        List<Pet> pets = petService.getAllPets();
        return copyPetsToPetsDTO(pets);
    }

    @GetMapping("/owner/{ownerId}")
    public List<PetRequest> getPetsByOwner(@PathVariable long ownerId) {
        List<Pet> pets = petService.findPetByOwner(Long.valueOf(ownerId));
        return copyPetsToPetsDTO(pets);
    }

    private List<PetRequest> copyPetsToPetsDTO(List<Pet> pets) {
        List<PetRequest> dtos = new ArrayList<>();
        pets.forEach(pet -> {
            dtos.add(new PetRequest(
                    pet.getId().longValue(),
                    pet.getType(),
                    pet.getName(),
                    pet.getOwner().getId().longValue(),
                    pet.getBirthDate(),
                    pet.getNotes()));
        });

        return dtos;
    }
}
