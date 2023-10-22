package kitchenpos.application;

import kitchenpos.dao.MenuGroupDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.ProductDao;
import kitchenpos.domain.*;
import kitchenpos.fixture.MenuProductFixtures;
import kitchenpos.fixture.OrderFixtures;
import kitchenpos.fixture.OrderLineItemFixtures;
import kitchenpos.request.MenuCreateRequest;
import kitchenpos.request.MenuProductDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static kitchenpos.fixture.MenuGroupFixtures.한마리_메뉴;
import static kitchenpos.fixture.OrderTableFixtures.createEmptyTable;
import static kitchenpos.fixture.ProductFixtures.양념치킨_17000원;
import static kitchenpos.fixture.TableGroupFixtures.createTableGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class TableGroupServiceTest extends ServiceTest {

    @Autowired
    MenuGroupDao menuGroupDao;
    @Autowired
    ProductDao productDao;
    @Autowired
    OrderDao orderDao;
    @Autowired
    OrderTableDao orderTableDao;

    @Autowired
    TableGroupService tableGroupService;
    @Autowired
    TableService tableService;
    @Autowired
    MenuService menuService;

    @DisplayName("단체 테이블을 지정한다.")
    @Test
    void create() {
        // given
        OrderTable orderTable1 = tableService.create(createEmptyTable());
        OrderTable orderTable2 = tableService.create(createEmptyTable());
        TableGroup tableGroup = createTableGroup(orderTable1, orderTable2);

        // when
        TableGroup savedTableGroup = tableGroupService.create(tableGroup);

        // then
        assertThat(savedTableGroup.getId()).isNotNull();
    }

    @DisplayName("단체 테이블로 지정하려는 테이블이 2개 미만이면 예외가 발생한다.")
    @Test
    void create_TablesSizeLessThanTwo_ExceptionThrown() {
        // given
        OrderTable orderTable = tableService.create(createEmptyTable());
        TableGroup tableGroup = createTableGroup(orderTable);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("단체 테이블로 지정하려는 테이블 중 빈 테이블이 아닌 테이블이 포함됐다면 예외가 발생한다.")
    @Test
    void create_ContainsNotEmptyTable_ExceptionThrown() {
        // given
        OrderTable notEmptyTable = tableService.create(new OrderTable());
        OrderTable emptyTable = tableService.create(createEmptyTable());
        TableGroup tableGroup = createTableGroup(notEmptyTable, emptyTable);

        // when, then
        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("단체 지정을 해제한다.")
    @Test
    void ungroup() {
        // given
        OrderTable orderTable1 = tableService.create(createEmptyTable());
        OrderTable orderTable2 = tableService.create(createEmptyTable());
        TableGroup savedTableGroup = tableGroupService.create(
                createTableGroup(orderTable1, orderTable2)
        );

        // when
        tableGroupService.ungroup(savedTableGroup.getId());
        OrderTable findOrderTable1 = orderTableDao.findById(orderTable1.getId()).get();
        OrderTable findOrderTable2 = orderTableDao.findById(orderTable2.getId()).get();

        // then
        assertAll(
                () -> assertThat(findOrderTable1.getTableGroupId()).isNull(),
                () -> assertThat(findOrderTable2.getTableGroupId()).isNull()
        );
    }

    @DisplayName("단체로 지정된 테이블 중, 주문상태가 조리중이거나 식사중인 테이블은 단체 지정을 해제할 수 없다.")
    @EnumSource(value = OrderStatus.class, names = {"COOKING", "MEAL"})
    @ParameterizedTest
    void ungroup_ContainsInvalidOrderProcessTable_ExceptionThrown(OrderStatus orderStatus) {
        // given
        OrderTable orderTable1 = tableService.create(createEmptyTable());
        OrderTable orderTable2 = tableService.create(createEmptyTable());
        TableGroup savedTableGroup = tableGroupService.create(
                createTableGroup(orderTable1, orderTable2)
        );

        Menu menu = menuService.create(getMenuCreateRequest("양념치킨", 17_000));
        Order order = createOrder(orderTable1, orderStatus, menu);
        Order savedOrder = orderDao.save(order);

        // when, then
        assertThatThrownBy(() -> tableGroupService.ungroup(savedTableGroup.getId()))
                .isInstanceOf(IllegalArgumentException.class);

    }

    private MenuCreateRequest getMenuCreateRequest(String name, int price) {
        Product product = productDao.save(양념치킨_17000원);
        MenuProduct menuProduct = MenuProductFixtures.create(product, 1);
        MenuGroup menuGroup = menuGroupDao.save(한마리_메뉴);
        return new MenuCreateRequest(
                name,
                BigDecimal.valueOf(price),
                menuGroup.getId(),
                MenuProductDto.of(List.of(menuProduct))
        );
    }

    private Order createOrder(OrderTable orderTable, OrderStatus orderStatus, Menu menu) {
        return OrderFixtures.create(
                orderTable.getId(),
                orderStatus.name(),
                LocalDateTime.now(),
                List.of(OrderLineItemFixtures.create(menu.getId(), 1))
        );
    }
}
