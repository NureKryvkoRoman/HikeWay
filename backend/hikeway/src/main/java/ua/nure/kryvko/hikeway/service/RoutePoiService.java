package ua.nure.kryvko.hikeway.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.model.PointOfInterest;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RoutePoi;
import ua.nure.kryvko.hikeway.model.response.PoiResponses;
import ua.nure.kryvko.hikeway.repository.PointOfInterestRepository;
import ua.nure.kryvko.hikeway.repository.RoutePoiRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoutePoiService {
    private final RoutePoiRepository routePoiRepository;
    private final PointOfInterestRepository poiRepository;
    private final PointOfInterestService poiService;

    public RoutePoiService(
            RoutePoiRepository routePoiRepository,
            PointOfInterestRepository poiRepository,
            PointOfInterestService poiService
    ) {
        this.routePoiRepository = routePoiRepository;
        this.poiRepository = poiRepository;
        this.poiService = poiService;
    }

    @Transactional
    public void replace(Route route, List<Long> requestedIds) {
        List<Long> poiIds = requestedIds == null ? List.of() : requestedIds;
        if (new HashSet<>(poiIds).size() != poiIds.size()) {
            throw new InvalidPoiDataException("INVALID_POI_REFERENCE", "A route cannot contain duplicate points of interest");
        }
        Map<Long, PointOfInterest> pois = poiRepository.findAllByIdInAndDeletedFalse(poiIds).stream()
                .collect(Collectors.toMap(PointOfInterest::getId, Function.identity()));
        if (pois.size() != poiIds.size()) {
            throw new InvalidPoiDataException("INVALID_POI_REFERENCE", "Route references an unknown point of interest");
        }

        routePoiRepository.deleteByRouteId(route.getId());
        List<RoutePoi> associations = new ArrayList<>();
        for (int position = 0; position < poiIds.size(); position++) {
            RoutePoi association = new RoutePoi();
            association.setRoute(route);
            association.setPoi(pois.get(poiIds.get(position)));
            association.setPosition(position);
            associations.add(association);
        }
        routePoiRepository.saveAll(associations);
    }

    @Transactional(readOnly = true)
    public List<Long> ids(long routeId) {
        return routePoiRepository.findByRouteIdOrderByPositionAsc(routeId).stream()
                .filter(routePoi -> !routePoi.getPoi().isDeleted())
                .map(routePoi -> routePoi.getPoi().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PoiResponses.Summary> summaries(long routeId) {
        return routePoiRepository.findByRouteIdOrderByPositionAsc(routeId).stream()
                .map(RoutePoi::getPoi)
                .filter(poi -> !poi.isDeleted())
                .map(poiService::toSummary)
                .toList();
    }

    @Transactional
    public void deleteForRoute(long routeId) {
        routePoiRepository.deleteByRouteId(routeId);
    }
}
