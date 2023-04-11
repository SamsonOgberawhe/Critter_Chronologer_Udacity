package com.udacity.jdnd.course3.critter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.udacity.jdnd.course3.critter.controller.UserController;
import com.udacity.jdnd.course3.critter.request.*;
import com.udacity.jdnd.course3.critter.request.EmployeeRequest;
import com.udacity.jdnd.course3.critter.entity.*;
import com.udacity.jdnd.course3.critter.controller.PetController;
import com.udacity.jdnd.course3.critter.controller.ScheduleController;
import com.udacity.jdnd.course3.critter.exceptions.EmployeeNotFoundException;
import com.udacity.jdnd.course3.critter.exceptions.PetNotFoundException;
import com.udacity.jdnd.course3.critter.service.PetService;
import com.udacity.jdnd.course3.critter.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a set of functional tests to validate the basic capabilities desired for this application.
 * Students will need to configure the application to run these tests by adding application.properties file
 * to the test/resources directory that specifies the datasource. It can run using an in-memory H2 instance
 * and should not try to re-use the same datasource used by the rest of the app.
 *
 * These tests should all pass once the project is complete.
 */
@Transactional
@SpringBootTest(classes = CritterApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CritterFunctionalTest {

    @Autowired
    private UserController userController;

    @Autowired
    private PetController petController;

    @Autowired
    private ScheduleController scheduleController;

    /**
     *  Service Layer classes added for additional tests.
     */
    @Autowired
    private PetService petService;

    @Autowired
    private UserService userService;

    @Test
    @Order(1)
    public void testCreateCustomer(){
        CustomerRequest customerRequest = createCustomerDTO();
        CustomerRequest newCustomer = userController.saveCustomer(customerRequest);
        CustomerRequest retrievedCustomer = userController.getAllCustomers().get(0);
        Assertions.assertEquals(newCustomer.getName(), customerRequest.getName());
        Assertions.assertEquals(newCustomer.getId(), retrievedCustomer.getId());
        Assertions.assertTrue(retrievedCustomer.getId() > 0);
    }

    @Test
    @Order(2)
    public void testCreateEmployee(){
        EmployeeRequest employeeRequest = createEmployeeDTO();
        EmployeeRequest newEmployee = userController.saveEmployee(employeeRequest);
        EmployeeRequest retrievedEmployee = userController.getEmployee(newEmployee.getId());
        Assertions.assertEquals(employeeRequest.getSkills(), newEmployee.getSkills());
        Assertions.assertEquals(newEmployee.getId(), retrievedEmployee.getId());
        Assertions.assertTrue(retrievedEmployee.getId() > 0);
    }

    @Test
    @Order(3)
    public void testAddPetsToCustomer() {
        CustomerRequest customerRequest = createCustomerDTO();
        CustomerRequest newCustomer = userController.saveCustomer(customerRequest);

        PetRequest petRequest = createPetDTO();
        petRequest.setOwnerId(newCustomer.getId());
        PetRequest newPet = petController.savePet(petRequest);

        //make sure pet contains customer id
        PetRequest retrievedPet = petController.getPet(newPet.getId());
        Assertions.assertEquals(retrievedPet.getId(), newPet.getId());
        Assertions.assertEquals(retrievedPet.getOwnerId(), newCustomer.getId());

        //make sure you can retrieve pets by owner
        List<PetRequest> pets = petController.getPetsByOwner(newCustomer.getId());
        Assertions.assertEquals(newPet.getId(), pets.get(0).getId());
        Assertions.assertEquals(newPet.getName(), pets.get(0).getName());

        //check to make sure customer now also contains pet
        CustomerRequest retrievedCustomer = userController.getAllCustomers().get(0);
        Assertions.assertTrue(retrievedCustomer.getPetIds() != null && retrievedCustomer.getPetIds().size() > 0);
        Assertions.assertEquals(retrievedCustomer.getPetIds().get(0), retrievedPet.getId());
    }

    @Test
    @Order(4)
    public void testFindPetsByOwner() {
        CustomerRequest customerRequest = createCustomerDTO();
        CustomerRequest newCustomer = userController.saveCustomer(customerRequest);

        PetRequest petRequest = createPetDTO();
        petRequest.setOwnerId(newCustomer.getId());
        PetRequest newPet = petController.savePet(petRequest);
        petRequest.setType(PetType.DOG);
        petRequest.setName("DogName");
        PetRequest newPet2 = petController.savePet(petRequest);

        List<PetRequest> pets = petController.getPetsByOwner(newCustomer.getId());
        Assertions.assertEquals(pets.size(), 2);
        Assertions.assertEquals(pets.get(0).getOwnerId(), newCustomer.getId());
        Assertions.assertEquals(pets.get(0).getId(), newPet.getId());
    }

    @Test
    @Order(5)
    public void testFindOwnerByPet() {
        CustomerRequest customerRequest = createCustomerDTO();
        CustomerRequest newCustomer = userController.saveCustomer(customerRequest);

        PetRequest petRequest = createPetDTO();
        petRequest.setOwnerId(newCustomer.getId());
        PetRequest newPet = petController.savePet(petRequest);

        CustomerRequest owner = userController.getOwnerByPet(newPet.getId());
        Assertions.assertEquals(owner.getId(), newCustomer.getId());
        Assertions.assertEquals(owner.getPetIds().get(0), newPet.getId());
    }

    @Test
    @Order(6)
    public void testChangeEmployeeAvailability() {
        EmployeeRequest employeeRequest = createEmployeeDTO();
        EmployeeRequest emp1 = userController.saveEmployee(employeeRequest);
        Assertions.assertNull(emp1.getDaysAvailable());

        Set<DayOfWeek> availability = Sets.newHashSet(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
        userController.setAvailability(availability, emp1.getId());

        EmployeeRequest emp2 = userController.getEmployee(emp1.getId());
        Assertions.assertEquals(availability, emp2.getDaysAvailable());
    }

    @Test
    @Order(7)
    public void testFindEmployeesByServiceAndTime() {
        EmployeeRequest emp1 = createEmployeeDTO();
        EmployeeRequest emp2 = createEmployeeDTO();
        EmployeeRequest emp3 = createEmployeeDTO();

        emp1.setDaysAvailable(Sets.newHashSet(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY));
        emp2.setDaysAvailable(Sets.newHashSet(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        emp3.setDaysAvailable(Sets.newHashSet(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        emp1.setSkills(Sets.newHashSet(EmployeeSkill.FEEDING, EmployeeSkill.PETTING));
        emp2.setSkills(Sets.newHashSet(EmployeeSkill.PETTING, EmployeeSkill.WALKING));
        emp3.setSkills(Sets.newHashSet(EmployeeSkill.WALKING, EmployeeSkill.SHAVING));

        EmployeeRequest emp1n = userController.saveEmployee(emp1);
        EmployeeRequest emp2n = userController.saveEmployee(emp2);
        EmployeeRequest emp3n = userController.saveEmployee(emp3);

        //make a request that matches employee 1 or 2
        EmployeeRequestDTO er1 = new EmployeeRequestDTO();
        er1.setDate(LocalDate.of(2019, 12, 25)); //wednesday
        er1.setSkills(Sets.newHashSet(EmployeeSkill.PETTING));

        Set<Long> eIds1 = userController.findEmployeesForService(er1).stream().map(EmployeeRequest::getId).collect(Collectors.toSet());
        Set<Long> eIds1expected = Sets.newHashSet(emp1n.getId(), emp2n.getId());
        Assertions.assertEquals(eIds1, eIds1expected);

        //make a request that matches only employee 3
        EmployeeRequestDTO er2 = new EmployeeRequestDTO();
        er2.setDate(LocalDate.of(2019, 12, 27)); //friday
        er2.setSkills(Sets.newHashSet(EmployeeSkill.WALKING, EmployeeSkill.SHAVING));

        Set<Long> eIds2 = userController.findEmployeesForService(er2).stream().map(EmployeeRequest::getId).collect(Collectors.toSet());
        Set<Long> eIds2expected = Sets.newHashSet(emp3n.getId());
        Assertions.assertEquals(eIds2, eIds2expected);
    }

    @Test
    @Order(8)
    public void testSchedulePetsForServiceWithEmployee() {
        EmployeeRequest employeeTemp = createEmployeeDTO();
        employeeTemp.setDaysAvailable(Sets.newHashSet(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY));
        EmployeeRequest employeeRequest = userController.saveEmployee(employeeTemp);
        CustomerRequest customerRequest = userController.saveCustomer(createCustomerDTO());
        PetRequest petTemp = createPetDTO();
        petTemp.setOwnerId(customerRequest.getId());
        PetRequest petRequest = petController.savePet(petTemp);

        LocalDate date = LocalDate.of(2019, 12, 25);
        List<Long> petList = Lists.newArrayList(petRequest.getId());
        List<Long> employeeList = Lists.newArrayList(employeeRequest.getId());
        Set<EmployeeSkill> skillSet =  Sets.newHashSet(EmployeeSkill.PETTING);

        scheduleController.createSchedule(createScheduleDTO(petList, employeeList, date, skillSet));
        ScheduleRequest scheduleRequest = scheduleController.getAllSchedules().get(0);

        Assertions.assertEquals(scheduleRequest.getActivities(), skillSet);
        Assertions.assertEquals(scheduleRequest.getDate(), date);
        Assertions.assertEquals(scheduleRequest.getEmployeeIds(), employeeList);
        Assertions.assertEquals(scheduleRequest.getPetIds(), petList);
    }

    @Test
    @Order(9)
    public void testFindScheduleByEntities() {
        ScheduleRequest sched1 = populateSchedule(1, 2, LocalDate.of(2019, 12, 25), Sets.newHashSet(EmployeeSkill.FEEDING, EmployeeSkill.WALKING));
        ScheduleRequest sched2 = populateSchedule(3, 1, LocalDate.of(2019, 12, 26), Sets.newHashSet(EmployeeSkill.PETTING));

        //add a third schedule that shares some employees and pets with the other schedules
        ScheduleRequest sched3 = new ScheduleRequest();
        sched3.setEmployeeIds(sched1.getEmployeeIds());
        sched3.setPetIds(sched2.getPetIds());

        sched3.setActivities(Sets.newHashSet(EmployeeSkill.SHAVING, EmployeeSkill.PETTING));
        sched3.setDate(LocalDate.of(2020, 3, 23));
        scheduleController.createSchedule(sched3);

        /*
            We now have 3 schedule entries. The third schedule entry has the same employees as the 1st schedule
            and the same pets/owners as the second schedule. So if we look up schedule entries for the employee from
            schedule 1, we should get both the first and third schedule as our result.
         */

        //Employee 1 in is both schedule 1 and 3
        List<ScheduleRequest> scheds1e = scheduleController.getScheduleForEmployee(sched1.getEmployeeIds().get(0));
        compareSchedules(sched1, scheds1e.get(0));
        compareSchedules(sched3, scheds1e.get(1));

        //Employee 2 is only in schedule 2
        List<ScheduleRequest> scheds2e = scheduleController.getScheduleForEmployee(sched2.getEmployeeIds().get(0));
        compareSchedules(sched2, scheds2e.get(0));

        //Pet 1 is only in schedule 1
        List<ScheduleRequest> scheds1p = scheduleController.getScheduleForPet(sched1.getPetIds().get(0));
        compareSchedules(sched1, scheds1p.get(0));

        //Pet from schedule 2 is in both schedules 2 and 3
        List<ScheduleRequest> scheds2p = scheduleController.getScheduleForPet(sched2.getPetIds().get(0));
        compareSchedules(sched2, scheds2p.get(0));
        compareSchedules(sched3, scheds2p.get(1));

        //Owner of the first pet will only be in schedule 1
        List<ScheduleRequest> scheds1c = scheduleController.getScheduleForCustomer(userController.getOwnerByPet(sched1.getPetIds().get(0)).getId());
        compareSchedules(sched1, scheds1c.get(0));

        //Owner of pet from schedule 2 will be in both schedules 2 and 3
        List<ScheduleRequest> scheds2c = scheduleController.getScheduleForCustomer(userController.getOwnerByPet(sched2.getPetIds().get(0)).getId());
        compareSchedules(sched2, scheds2c.get(0));
        compareSchedules(sched3, scheds2c.get(1));
    }

    @Test
    @DisplayName("Additional Test: PetNotFoundException message")
    @Order(10)
    public void testPetNotFoundException (){
        Long nonExistingId = 1000L;
        String expectedMessage = "Could not find pet(s) with id(s): " + nonExistingId;
        String actualMessage = null;
        Customer customer = createCustomer();
        List<Long> idList = new ArrayList<>();
        customer = userService.save(customer, idList);
        Pet pet = createPet("Figaro", PetType.CAT);
        pet = petService.save(pet, customer.getId());
        idList.add(pet.getId());
        idList.add(nonExistingId);

        Assertions.assertThrows(PetNotFoundException.class, () -> {
            petService.findPets(idList);
        });
        // one missing id
        try {
            petService.findPets(idList);
        } catch (PetNotFoundException petNotFoundException){
            actualMessage = petNotFoundException.getMessage();
        }
        Assertions.assertEquals(expectedMessage, actualMessage);

        // two missing ids
        nonExistingId++;
        expectedMessage += ", " + nonExistingId;
        idList.add(nonExistingId);
        try {
            petService.findPets(idList);
        } catch (PetNotFoundException petNotFoundException){
            actualMessage = petNotFoundException.getMessage();
        }
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("Additional Test: EmployeeNotFoundException message")
    @Order(11)
    public void testEmployeeNotFoundExceptionMessage (){
        Long nonExistingId = 1000L;
        String expectedMessage = "Could not find employee(s) with id(s): " + nonExistingId;
        String actualMessage = null;
        Employee employee = createEmployee();
        employee = userService.save(employee);
        List<Long> idList = new ArrayList<>();
        idList.add(employee.getId());
        idList.add(nonExistingId);

        Assertions.assertThrows(EmployeeNotFoundException.class, () -> {
            userService.findAllEmployees(idList);
        });
        // one missing id
        try {
            userService.findAllEmployees(idList);
        } catch (EmployeeNotFoundException employeeNotFoundException){
            actualMessage = employeeNotFoundException.getMessage();
        }
        Assertions.assertEquals(expectedMessage, actualMessage);

        // two missing ids
        nonExistingId++;
        expectedMessage += ", " + nonExistingId;
        idList.add(nonExistingId);
        try {
            userService.findAllEmployees(idList);
        } catch (EmployeeNotFoundException employeeNotFoundException){
            actualMessage = employeeNotFoundException.getMessage();
        }
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    private static Pet createPet(String name, PetType type) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setType(type);
        return pet;
    }

    private static Employee createEmployee() {
        Employee employee = new Employee();
        employee.setName("TestEmployee");
        return employee;
    }

    private static Customer createCustomer() {
        Customer customer = new Customer();
        customer.setName("TestEmployee");
        customer.setPhoneNumber("123-456-789");
        return customer;
    }

    private static EmployeeRequest createEmployeeDTO() {
        EmployeeRequest employeeRequest = new EmployeeRequest();
        employeeRequest.setName("TestEmployee");
        employeeRequest.setSkills(Sets.newHashSet(EmployeeSkill.FEEDING, EmployeeSkill.PETTING));
        return employeeRequest;
    }
    private static CustomerRequest createCustomerDTO() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("TestEmployee");
        customerRequest.setPhoneNumber("123-456-789");
        return customerRequest;
    }

    private static PetRequest createPetDTO() {
        PetRequest petRequest = new PetRequest();
        petRequest.setName("TestPet");
        petRequest.setType(PetType.CAT);
        return petRequest;
    }

    private static EmployeeRequestDTO createEmployeeRequestDTO() {
        EmployeeRequestDTO employeeRequestDTO = new EmployeeRequestDTO();
        employeeRequestDTO.setDate(LocalDate.of(2019, 12, 25));
        employeeRequestDTO.setSkills(Sets.newHashSet(EmployeeSkill.FEEDING, EmployeeSkill.WALKING));
        return employeeRequestDTO;
    }

    private static ScheduleRequest createScheduleDTO(List<Long> petIds, List<Long> employeeIds, LocalDate date, Set<EmployeeSkill> activities) {
        ScheduleRequest scheduleRequest = new ScheduleRequest();
        scheduleRequest.setPetIds(petIds);
        scheduleRequest.setEmployeeIds(employeeIds);
        scheduleRequest.setDate(date);
        scheduleRequest.setActivities(activities);
        return scheduleRequest;
    }

    private ScheduleRequest populateSchedule(int numEmployees, int numPets, LocalDate date, Set<EmployeeSkill> activities) {
        List<Long> employeeIds = IntStream.range(0, numEmployees)
                .mapToObj(i -> createEmployeeDTO())
                .map(e -> {
                    e.setSkills(activities);
                    e.setDaysAvailable(Sets.newHashSet(date.getDayOfWeek()));
                    return userController.saveEmployee(e).getId();
                }).collect(Collectors.toList());
        CustomerRequest cust = userController.saveCustomer(createCustomerDTO());
        List<Long> petIds = IntStream.range(0, numPets)
                .mapToObj(i -> createPetDTO())
                .map(p -> {
                    p.setOwnerId(cust.getId());
                    return petController.savePet(p).getId();
                }).collect(Collectors.toList());
        return scheduleController.createSchedule(createScheduleDTO(petIds, employeeIds, date, activities));
    }

    private static void compareSchedules(ScheduleRequest sched1, ScheduleRequest sched2) {
        Assertions.assertEquals(sched1.getPetIds(), sched2.getPetIds());
        Assertions.assertEquals(sched1.getActivities(), sched2.getActivities());
        Assertions.assertEquals(sched1.getEmployeeIds(), sched2.getEmployeeIds());
        Assertions.assertEquals(sched1.getDate(), sched2.getDate());
    }

}
