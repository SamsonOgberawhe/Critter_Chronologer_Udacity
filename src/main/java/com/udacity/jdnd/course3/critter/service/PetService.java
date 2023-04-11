package com.udacity.jdnd.course3.critter.service;

import com.udacity.jdnd.course3.critter.entity.Customer;
import com.udacity.jdnd.course3.critter.entity.Pet;
import com.udacity.jdnd.course3.critter.exceptions.CustomerNotFoundException;
import com.udacity.jdnd.course3.critter.repository.CustomerRepository;
import com.udacity.jdnd.course3.critter.repository.PetRepository;
import com.udacity.jdnd.course3.critter.exceptions.PetNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PetService {

    @Autowired
    PetRepository petRepository;

    @Autowired
    CustomerRepository customerRepository;

    public Optional<Pet> getPet(Long id) {
        return petRepository.findById(id);
    }

    public List<Pet> findPetByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }

    public List<Pet> findPets(List<Long> petIds) {
        List<Pet> listOfPets = petRepository.findAllById(petIds);

        if (petIds.size() != listOfPets.size()) {
            List<Long> found = listOfPets.stream().map(p -> p.getId()).collect(Collectors.toList());
            String missingPets = (String) petIds
                    .stream()
                    .filter( id -> !found.contains(id) )
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new PetNotFoundException("Pet(s) with id(s) could not be found: " + missingPets);
        }
        return listOfPets;
    }

    public List<Pet> getAllPets() {
        return petRepository.findAll();
    }

    @Transactional
    public Pet save(Pet p, Long ownerId) throws CustomerNotFoundException {
        // find teh owner
        Customer owner = customerRepository.findById(ownerId)
                .orElseThrow(() -> new CustomerNotFoundException("ID: " + ownerId));

        // This line adds the owner of the pet and saves it
        p.setOwner(owner);
        p = petRepository.save(p);

        // This line updates the owner with the new pet and saves
        owner.getPets().add(p);
        customerRepository.save(owner);

        return p;
    }
}
