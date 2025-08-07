package com.networkflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web controller for serving HTML pages
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "redirect:/workflows";
    }

    @GetMapping("/workflows")
    public String workflowList(Model model) {
        model.addAttribute("pageTitle", "Workflow List");
        return "workflows/list";
    }

    @GetMapping("/workflows/new")
    public String newWorkflow(Model model) {
        model.addAttribute("pageTitle", "New Workflow");
        return "workflows/editor";
    }

    @GetMapping("/workflows/{id}")
    public String editWorkflow(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Edit Workflow");
        model.addAttribute("workflowId", id);
        return "workflows/editor";
    }

    @GetMapping("/templates")
    public String templateList(Model model) {
        model.addAttribute("pageTitle", "Templates");
        return "templates/list";
    }

    @GetMapping("/templates/new")
    public String newTemplate(Model model) {
        model.addAttribute("pageTitle", "New Template");
        return "templates/editor";
    }

    @GetMapping("/templates/{id}")
    public String editTemplate(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Edit Template");
        model.addAttribute("templateId", id);
        return "templates/editor";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About");
        return "about";
    }

    @GetMapping("/output-parsing")
    public String outputParsingList() {
        return "output-parsing/list";
    }
    
    @GetMapping("/output-parsing/new")
    public String newOutputParsingTemplate() {
        return "output-parsing/editor";
    }
    
    @GetMapping("/output-parsing/{id}")
    public String editOutputParsingTemplate(@PathVariable String id, Model model) {
        model.addAttribute("templateId", id);
        return "output-parsing/editor";
    }
    
    @GetMapping("/output-parsing/test")
    public String testOutputParsing() {
        return "output-parsing/test";
    }
}