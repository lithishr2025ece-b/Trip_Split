package com.example.tripsplit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {
    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/trips")
    public String trips() { return "trips"; }

    @GetMapping("/trips/create")
    public String createTrip() { return "create-trip"; }

    @GetMapping("/trips/{id}")
    public String tripDetails(@PathVariable Long id) { return "trip-details"; }

    @GetMapping("/trips/{id}/participant")
    public String addParticipant(@PathVariable Long id) { return "add-participant"; }

    @GetMapping("/trips/{id}/expense")
    public String addExpense(@PathVariable Long id) { return "add-expense"; }

    @GetMapping("/trips/{id}/balance")
    public String balances(@PathVariable Long id) { return "balances"; }

    @GetMapping("/trips/{id}/settlement")
    public String settlements(@PathVariable Long id) { return "settlements"; }
}
