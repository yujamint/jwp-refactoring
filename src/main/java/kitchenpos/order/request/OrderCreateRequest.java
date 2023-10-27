package kitchenpos.order.request;

import java.util.List;
import javax.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotNull
    private final Long orderTableId;
    @NotNull
    private final List<OrderLineItemDto> orderLineItems;

    public OrderCreateRequest(Long orderTableId, List<OrderLineItemDto> orderLineItems) {
        this.orderTableId = orderTableId;
        this.orderLineItems = orderLineItems;
    }

    public Long getOrderTableId() {
        return orderTableId;
    }

    public List<OrderLineItemDto> getOrderLineItems() {
        return orderLineItems;
    }
}
