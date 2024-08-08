package io.fiap.fastfood.driven.repository;

import io.fiap.fastfood.driven.core.entity.OrderTrackingEntity;
import io.fiap.fastfood.driven.core.entity.PaginatedOrderTrackingEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderTrackingRepository extends ReactiveCrudRepository<OrderTrackingEntity, String> {
    Flux<OrderTrackingEntity> findByOrderNumberOrderByOrderDateTime(String orderId);

    @Aggregation({"{" +
        "      '$group':{" +
        "         'start':{" +
        "            '$first':'$$ROOT'" +
        "         }," +
        "         'end':{" +
        "            '$last':'$$ROOT'" +
        "         }," +
        "         '_id':{" +
        "            '_id':'$orderId'" +
        "         }" +
        "      }" +
        "   }",
        "   {" +
            "      '$addFields':{" +
            "         'orderStatus':'$end.orderStatus'," +
            "         'orderStatusValue':'$end.orderStatusValue',"+
            "         'orderId':'$end.orderId'," +
            "         'orderNumber':'$end.orderNumber'," +
            "         'orderDateTime':'$end.orderDateTime'," +
            "         'orderTimeSpent':{" +
            "            '$dateDiff':{" +
            "               'startDate':'$start.orderDateTime'," +
            "               'endDate':'$end.orderDateTime'," +
            "               'unit':'minute'" +
            "            }" +
            "         }" +
            "      }" +
            "   }",
        "   {" +
            "      '$project':{" +
            "         'orderNumber':'$orderStatus'," +
            "         'orderId':'$orderId'," +
            "         'orderNumber':'$orderNumber'," +
            "         'orderDateTime':'$orderDateTime'," +
            "         'orderTimeSpent':'$orderTimeSpent'" +
            "      }" +
            "   }",
        "   {" +
            "      '$sort':{" +
            "         'orderStatusValue': -1, " +
            "         'orderDateTime': 1" +
            "      }" +
            "   }",
        "   {" +
            "      '$facet':{" +
            "         'metadata':[" +
            "            {" +
            "               '$count':'total'" +
            "            }," +
            "            {" +
            "               '$addFields':{" +
            "                  'page': :#{#page}" +
            "               }" +
            "            }" +
            "         ]," +
            "         'data':[" +
            "            {" +
            "               '$skip': :#{#skip}" +
            "            }," +
            "            {" +
            "               '$limit': :#{#limit}" +
            "            }" +
            "         ]" +
            "      }" +
            "   }"})
    Mono<PaginatedOrderTrackingEntity> findTracking(@Param("page") Integer page, @Param("skip") Integer skip, @Param("limit") Integer limit);
}
