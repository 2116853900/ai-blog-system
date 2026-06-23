package com.aiblog.controller;

import com.aiblog.dto.RelatedResourceResponse;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.service.RelatedResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/related-resources")
public class RelatedResourceController {

    private final RelatedResourceService service;

    public RelatedResourceController(RelatedResourceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<RelatedResourceResponse>> list(@RequestParam ResourceFavorite.RefType refType,
                                                              @RequestParam Long refId,
                                                              @RequestParam(defaultValue = "6") int limit) {
        return service.related(refType, refId, limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
