package com.udacity.jdnd.course3.critter.controller;

import com.udacity.jdnd.course3.critter.request.ScheduleRequest;
import com.udacity.jdnd.course3.critter.entity.Schedule;
import com.udacity.jdnd.course3.critter.exceptions.*;
import com.udacity.jdnd.course3.critter.service.PetService;
import com.udacity.jdnd.course3.critter.service.ScheduleService;
import com.udacity.jdnd.course3.critter.service.UserService;
import com.udacity.jdnd.course3.critter.service.ValidationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles web requests related to Schedules.
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private static final String []  PROPERTIES_TO_IGNORE_ON_COPY = { "id" };

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    UserService userService;

    @Autowired
    PetService petService;

    @Autowired
    ValidationService validationService;

    @PostMapping
    public ScheduleRequest createSchedule(@RequestBody ScheduleRequest scheduleRequest)
            throws EmployeeNotFoundException, PetNotFoundException,
            MissingInfoException {

        validationService.validatePOJOAttributesNotNullOrEmpty(scheduleRequest);

        Schedule s = scheduleService.findSchedule(scheduleRequest.getId()).orElseGet(Schedule::new);

        s.setDate(scheduleRequest.getDate());
        s.setActivities(scheduleRequest.getActivities());
        s.setEmployees(userService.findAllEmployees(scheduleRequest.getEmployeeIds()));
        s.setPets(petService.findPets(scheduleRequest.getPetIds()));

        s = scheduleService.save(s);

        return copyScheduleToDTO(s);
    }

    @GetMapping
    public List<ScheduleRequest> getAllSchedules() {
        List<Schedule> schedules = scheduleService.findAllSchedules();
        return copyScheduleToDTO(schedules);
    }

    @GetMapping("/pet/{petId}")
    public List<ScheduleRequest> getScheduleForPet(@PathVariable long petId) throws PetNotFoundException {
        return copyScheduleToDTO(scheduleService.findSchedulesForPet(petId));
    }

    @GetMapping("/employee/{employeeId}")
    public List<ScheduleRequest> getScheduleForEmployee(@PathVariable long employeeId) throws EmployeeNotFoundException {
        return copyScheduleToDTO(scheduleService.findSchedulesForEmployee(employeeId));
    }

    @GetMapping("/customer/{customerId}")
    public List<ScheduleRequest> getScheduleForCustomer(@PathVariable long customerId) throws CustomerNotFoundException {
        return copyScheduleToDTO(scheduleService.findSchedulesForCustomer(customerId));
    }

    private List<ScheduleRequest> copyScheduleToDTO(List<Schedule> schedules) {
        return schedules
                .stream()
                .map(s -> { return copyScheduleToDTO(s); })
                .collect(Collectors.toList());
    }

    private ScheduleRequest copyScheduleToDTO(Schedule s) {
        ScheduleRequest dto = new ScheduleRequest();
        BeanUtils.copyProperties(s, dto);
        s.getEmployees().forEach(employee -> {dto.getEmployeeIds().add(employee.getId());});
        s.getPets().forEach(pet -> {dto.getPetIds().add(pet.getId());});
        return dto;
    }
}
