/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.securityanalytics.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.securityanalytics.action.GetFindingsAction;
import org.opensearch.securityanalytics.action.GetFindingsRequest;
import org.opensearch.securityanalytics.action.GetFindingsResponse;
import org.opensearch.securityanalytics.action.SearchDetectorRequest;
import org.opensearch.securityanalytics.findings.FindingsService;
import org.opensearch.securityanalytics.model.Detector;
import org.opensearch.securityanalytics.util.DetectorUtils;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportGetFindingsAction extends HandledTransportAction<GetFindingsRequest, GetFindingsResponse> {

    private final TransportSearchDetectorAction transportSearchDetectorAction;

    private final NamedXContentRegistry xContentRegistry;

    private final FindingsService findingsService;

    private static final Logger log = LogManager.getLogger(TransportGetFindingsAction.class);


    @Inject
    public TransportGetFindingsAction(TransportService transportService, ActionFilters actionFilters, TransportSearchDetectorAction transportSearchDetectorAction, NamedXContentRegistry xContentRegistry, Client client) {
        super(GetFindingsAction.NAME, transportService, actionFilters, GetFindingsRequest::new);
        this.transportSearchDetectorAction = transportSearchDetectorAction;
        this.xContentRegistry = xContentRegistry;
        this.findingsService = new FindingsService(client);
    }

    @Override
    protected void doExecute(Task task, GetFindingsRequest request, ActionListener<GetFindingsResponse> actionListener) {
        if (request.getDetectorType() == null) {
            findingsService.getFindingsByDetectorId(request.getDetectorId(), request.getTable(), actionListener);
        } else {
            transportSearchDetectorAction.execute(new SearchDetectorRequest(new SearchRequest()), new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    try {
                        List<Detector> detectors = DetectorUtils.getDetectors(searchResponse, xContentRegistry);
                        findingsService.getFindings(detectors, request.getTable(), actionListener);
                    } catch (IOException e) {
                        actionListener.onFailure(e);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    actionListener.onFailure(e);
                }
            });

        }
    }
}