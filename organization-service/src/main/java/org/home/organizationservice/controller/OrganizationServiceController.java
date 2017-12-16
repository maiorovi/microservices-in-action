package org.home.organizationservice.controller;

import org.home.organizationservice.model.Organization;
import org.home.organizationservice.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( value = "v1/ogranizations")
public class OrganizationServiceController {
    @Autowired
    private OrganizationService organizationService;

    @RequestMapping(value = "/{organizationId}", method = RequestMethod.GET)
    public Organization getOrganization(@PathVariable("organizationId") String organizationId) {
        return organizationService.getOrg(organizationId);
    }

    @RequestMapping(value = "/{organizationId}", method = RequestMethod.PUT)
    public void updateOrganization(@PathVariable("organizationId") String organizationId, @RequestBody Organization org) {
        organizationService.updateOrg(org);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void saveOrganization(@RequestBody Organization org) {
        organizationService.saveOrg(org);
    }

    @RequestMapping(value = "/{organizationId}", method = RequestMethod.DELETE)
    public void deleteOrganization(@PathVariable("orgId") String orgId, @RequestBody Organization org) {
        organizationService.deleteOrg(org);
    }
}
