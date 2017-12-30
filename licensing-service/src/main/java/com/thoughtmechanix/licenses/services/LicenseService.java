package com.thoughtmechanix.licenses.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.thoughtmechanix.licenses.clients.OrganizationDiscoveryClient;
import com.thoughtmechanix.licenses.clients.OrganizationFeignClient;
import com.thoughtmechanix.licenses.clients.OrganizationRestTemplateClient;
import com.thoughtmechanix.licenses.config.ServiceConfig;
import com.thoughtmechanix.licenses.model.License;
import com.thoughtmechanix.licenses.model.Organization;
import com.thoughtmechanix.licenses.repository.LicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LicenseService {
    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ServiceConfig config;
    @Autowired
    OrganizationFeignClient organizationFeignClient;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;

    @Autowired
    OrganizationDiscoveryClient organizationDiscoveryClient;

    public License getLicense(String organizationId, String licenseId, String clientType) {
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);

        Organization org = retrieveOrgInfo(organizationId, clientType);
        log.info("Found organization with paramters {}", org);
        return license
                .withOrganizationName(org.getName())
                .withContactName(org.getContactName())
                .withContactEmail(org.getContactEmail())
                .withContactPhone(org.getContactPhone())
                .withComment(config.getExampleProperty());
    }

    private Organization retrieveOrgInfo(String organizationId, String clientType) {
        Organization organization = null;
        log.info("Looking up organization with id {} and clientType {}", organizationId, clientType);

        switch (clientType) {
            case "feign":
                System.out.println("I am using the feign client");
                organization = organizationFeignClient.getOrganization(organizationId);
                break;
            case "rest":
                System.out.println("I am using the rest client");
                organization = organizationRestClient.getOrganization(organizationId);
                break;
            case "discovery":
                System.out.println("I am using the discovery client");
                organization = organizationDiscoveryClient.getOrganization(organizationId);
                break;
            default:
                organization = organizationRestClient.getOrganization(organizationId);
        }

        return organization;
    }

    @HystrixCommand(
            commandProperties = {
                    @HystrixProperty(
                            name = "execution.isolation.thread.timeoutInMilliseconds",
                            value = "1000"
                            )},
            fallbackMethod = "buildFallbackLicenseList",
            threadPoolKey = "licensesByOrgThreadPool",
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value="30"),
                    @HystrixProperty(name = "maxQueueSize", value="10")
            }
    )
    public List<License> getLicensesByOrg(String organizationId) {
        randomlyRunLong();
        List<License> licenses = licenseRepository.findByOrganizationId(organizationId);
        final Organization org = retrieveOrgInfo(organizationId, "feign");

        return licenses.stream().map(
                license -> license
                        .withOrganizationId(org.getId())
                        .withOrganizationName(org.getName())
                        .withContactName(org.getContactName())
                        .withContactEmail(org.getContactEmail())
                        .withContactPhone(org.getContactPhone())
                        .withComment(config.getExampleProperty())).collect(Collectors.toList());
    }

    private void randomlyRunLong() {
        Random rand = new Random();
        int randomInt = rand.nextInt((3-1) + 1) + 1;
        if (randomInt == 3) sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<License> buildFallbackLicenseList(String organizationId) {
        List<License> fallbackList = new ArrayList<>();

        License lincense = new License()
                .withId("0000000000-00-0000")
                .withOrganizationId(organizationId)
                .withProductName("sorry no licensing information currently available");
        fallbackList.add(lincense);

        return fallbackList;
    }

    public void saveLicense(License license) {
        license.withId(UUID.randomUUID().toString());

        licenseRepository.save(license);

    }

    public void updateLicense(License license) {
        licenseRepository.save(license);
    }

    public void deleteLicense(License license) {
        licenseRepository.delete(license.getLicenseId());
    }

}
