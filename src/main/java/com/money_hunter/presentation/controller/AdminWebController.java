package com.money_hunter.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {
	@GetMapping({"/admin", "/admin/"})
	public String admin() {
		return "forward:/admin/index.html";
	}
}
