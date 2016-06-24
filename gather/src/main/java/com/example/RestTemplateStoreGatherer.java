/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/stores")
public class RestTemplateStoreGatherer {

    @Value("${app.url:http://stores.cfapps.io}")
    private String url = "http://stores.cfapps.io";

    private static Logger log = LoggerFactory.getLogger(RestTemplateStoreGatherer.class);
    private RestTemplate restTemplate = new RestTemplate();

    @RequestMapping
    public CompletableFuture<List<Store>> gather() {
        Scheduler scheduler = Schedulers.elastic();
        return Flux.range(0, Integer.MAX_VALUE) // <1>
                .flatMap(page -> Flux.defer(() -> page(page)).subscribeOn(scheduler), 2) // <2>
                .flatMap(store -> Mono.fromCallable(() -> meta(store))
                        .subscribeOn(scheduler), 4) // <3>
                .take(50) // <4>
                .collectList() // <5>
                .toFuture();
    }

    // <1> an "infinite" sequence sine we don't know how many pages there are
    // <2> drop to a background thread to process each page, 2 at a time
    // <3> for each store drop to a background thread to enhance it with metadata, 4 at a
    // time
    // <4> take at most 50
    // <5> convert to a list

    /**
     * Enhance a Store with some metadata. Blocking.
     *
     * @param store a Store to enhance
     * @return the enhanced store
     */
    private Store meta(Store store) {
        Map<String, Object> map = this.restTemplate
                .exchange(url + "/stores/{id}", HttpMethod.GET, null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }, store.getId())
                .getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) map.get("address");
        store.getMeta().putAll(meta);
        return store;
    }

    /**
     * Get a page of stores from the backend, Blocking.
     *
     * @param page the page number (starting at 0)
     * @return a page of stores (or empty)
     */
    private Flux<Store> page(int page) {
        Map<String, Object> map = this.restTemplate
                .exchange(url + "/stores?page={page}", HttpMethod.GET, null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }, page)
                .getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map<String, Object>) map
                .get("_embedded")).get("stores");
        List<Store> stores = new ArrayList<>();
        for (Map<String, Object> store : list) {
            stores.add(new Store((String) store.get("id"), (String) store.get("name")));
        }
        log.info("Fetched " + stores.size() + " stores for page: " + page);
        return Flux.fromIterable(stores);
    }

}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class Store {

    private String id;
    private String name;
    private Map<String, Object> meta = new LinkedHashMap<>();

    Store() {
    }

    @JsonCreator
    public Store(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getMeta() {
        return this.meta;
    }

}