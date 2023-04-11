package com.udacity.jdnd.course3.critter.controller;

import com.udacity.jdnd.course3.critter.request.CustomerRequest;
import com.udacity.jdnd.course3.critter.request.EmployeeRequest;
import com.udacity.jdnd.course3.critter.request.EmployeeRequestDTO;
import com.udacity.jdnd.course3.critter.entity.Customer;
import com.udacity.jdnd.course3.critter.entity.Employee;
import com.udacity.jdnd.course3.critter.entity.Pet;
import com.udacity.jdnd.course3.critter.exceptions.MissingInfoException;
import com.udacity.jdnd.course3.critter.exceptions.PetNotFoundException;
import com.udacity.jdnd.course3.critter.service.PetService;
import com.udacity.jdnd.course3.critter.service.UserService;
import com.udacity.jdnd.course3.critter.exceptions.EmployeeNotFoundException;
import com.udacity.jdnd.course3.critter.service.ValidationService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles web requests related to Users.
 *
 * Includes requests for both customers and employees. Splitting this into separate user and customer controllers
 * would be fine too, though that is not part of the required scope for this class.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final String []  PROPERTIES_TO_IGNORE_ON_COPY = { "id" };

    private UserService userService;

    private PetService petService;

    private ValidationService validationService;

    public UserController(UserService userService, PetService petService, ValidationService validationService) {
        this.userService = userService;
        this.petService = petService;
        this.validationService = validationService;
    }

    @PostMapping("/customer")
    public CustomerRequest saveCustomer(@RequestBody CustomerRequest customerRequest){
        Long id = Optional.ofNullable(customerRequest.getId()).orElse(Long.valueOf(-1));
        Customer c = userService.findCustomerById(id).orElseGet(Customer::new);
        BeanUtils.copyProperties(customerRequest, c, PROPERTIES_TO_IGNORE_ON_COPY);
        List<Long> petIds = Optional.ofNullable(customerRequest.getPetIds()).orElseGet(ArrayList::new);
        c = userService.save(c, petIds);
        return copyCustomerToDTO(c);
    }

    @GetMapping("/customer")
    public List<CustomerRequest> getAllCustomers(){
        List<Customer> customers = userService.getAllCustomers();
        return copyCustomersToDTOs(customers);
    }

    @GetMapping("/customer/pet/{petId}")
    public CustomerRequest getOwnerByPet(@PathVariable long petId) throws PetNotFoundException{
        Pet p = petService.getPet(petId).orElseThrow(() -> new PetNotFoundException("ID: " + petId));
        return copyCustomerToDTO(p.getOwner());
    }

    @PostMapping("/employee")
    public EmployeeRequest saveEmployee(@RequestBody EmployeeRequest employeeRequest) {
        Employee e = userService.findEmployee(employeeRequest.getId()).orElseGet(Employee::new);
        BeanUtils.copyProperties(employeeRequest, e, PROPERTIES_TO_IGNORE_ON_COPY);
        e = userService.save(e);
        return copyEmployeeToDTO(e);
    }

    @GetMapping("/employee/{employeeId}")
    public EmployeeRequest getEmployee(@PathVariable long employeeId) throws EmployeeNotFoundException {
        Employee e = userService.findEmployee(employeeId).orElseThrow(() -> new EmployeeNotFoundException("ID: " + employeeId));
        return copyEmployeeToDTO(e);
    }

    @GetMapping("/employees")
    public List<EmployeeRequest> getEmployees() {
        List<Employee> employees = userService.findAllEmployees();
        return employees.stream().map((e) -> {return copyEmployeeToDTO(e);}).collect(Collectors.toList());
    }

    @Transactional
    @PutMapping("/employee/{employeeId}")
    public void setAvailability(@RequestBody Set<DayOfWeek> daysAvailable, @PathVariable long employeeId) throws EmployeeNotFoundException {
        Employee e = userService.findEmployee(employeeId).orElseThrow(() -> new EmployeeNotFoundException("ID: " + employeeId));
        e.setDaysAvailable(daysAvailable);
        userService.save(e);
    }

    @GetMapping("/employee/availability")
    public List<EmployeeRequest> findEmployeesForService(@RequestBody EmployeeRequestDTO employeeRequestDTO) throws MissingInfoException {
        validationService.validatePOJOAttributesNotNullOrEmpty(employeeRequestDTO);
        List<Employee> employees = userService.findEmployeesAvailable(employeeRequestDTO.getSkills(), employeeRequestDTO.getDate());
        return employees.stream().map(this::copyEmployeeToDTO).collect(Collectors.toList());
    }

    private EmployeeRequest copyEmployeeToDTO(Employee employee) {
        EmployeeRequest dto = new EmployeeRequest();
        BeanUtils.copyProperties(employee, dto);
        return dto;
    }

    private CustomerRequest copyCustomerToDTO(Customer c){
        CustomerRequest dto = new CustomerRequest();
        BeanUtils.copyProperties(c, dto);
        c.getPets().forEach( pet -> {
            dto.getPetIds().add(pet.getId());
        });
        return dto;
    }

    private List<CustomerRequest> copyCustomersToDTOs (List<Customer> customers) {
        List dtos = new ArrayList<CustomerRequest>();
        // convert to DTO
        customers.forEach( c -> {
            dtos.add(this.copyCustomerToDTO((Customer)c));
        });
        return dtos;
    }

}
