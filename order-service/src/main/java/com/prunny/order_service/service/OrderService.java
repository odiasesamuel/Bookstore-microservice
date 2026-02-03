package com.prunny.order_service.service;

import book.ReduceAvailableCopiesResponse;
import com.prunny.order_service.domain.Order;
import com.prunny.order_service.event.publisher.BookEventPublisher;
import com.prunny.order_service.grpc.client.BookServiceGrpcClient;
import com.prunny.order_service.repository.OrderRepository;
import com.prunny.order_service.service.dto.OrderDTO;
import com.prunny.order_service.service.mapper.OrderMapper;
import java.util.Optional;

import com.prunny.order_service.web.rest.errors.InsufficientStockException;
import com.prunny.order_service.web.rest.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.prunny.order_service.domain.Order}.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final BookServiceGrpcClient bookServiceGrpcClient;

    private final BookEventPublisher bookEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, BookServiceGrpcClient bookServiceGrpcClient, BookEventPublisher bookEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.bookServiceGrpcClient = bookServiceGrpcClient;
        this.bookEventPublisher = bookEventPublisher;
    }

    /**
     * Save a order.
     *
     * @param orderDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDTO save(OrderDTO orderDTO) {
        LOG.debug("Request to save Order : {}", orderDTO);
        Order order = orderMapper.toEntity(orderDTO);
        order = orderRepository.save(order);

        ReduceAvailableCopiesResponse bookServiceResponse = bookServiceGrpcClient.reduceAvailableCopies(order.getBookIsbn(), order.getQuantity());
        LOG.info("Received response from book service via GRPC: {}", bookServiceResponse);
        if (!bookServiceResponse.getSuccess()) {
            String msg = bookServiceResponse.getMessage().toLowerCase();
            if (msg.contains("not found")) throw new ResourceNotFoundException(msg);
            if (msg.contains("not enough stock")) throw new InsufficientStockException(msg);
            throw new ResourceNotFoundException(bookServiceResponse.getMessage());
        }

        // Make an async request using kafka to book service to increment the sales count
        bookEventPublisher.publishBookOrderedEvent(
            order.getId().toString(),
            order.getBookIsbn(),
            order.getQuantity()
        );

        return orderMapper.toDto(order);
    }

    /**
     * Update a order.
     *
     * @param orderDTO the entity to save.
     * @return the persisted entity.
     */
    public OrderDTO update(OrderDTO orderDTO) {
        LOG.debug("Request to update Order : {}", orderDTO);
        Order order = orderMapper.toEntity(orderDTO);
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    /**
     * Partially update a order.
     *
     * @param orderDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrderDTO> partialUpdate(OrderDTO orderDTO) {
        LOG.debug("Request to partially update Order : {}", orderDTO);

        return orderRepository
            .findById(orderDTO.getId())
            .map(existingOrder -> {
                orderMapper.partialUpdate(existingOrder, orderDTO);

                return existingOrder;
            })
            .map(orderRepository::save)
            .map(orderMapper::toDto);
    }

    /**
     * Get all the orders.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Orders");
        return orderRepository.findAll(pageable).map(orderMapper::toDto);
    }

    /**
     * Get one order by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrderDTO> findOne(Long id) {
        LOG.debug("Request to get Order : {}", id);
        return orderRepository.findById(id).map(orderMapper::toDto);
    }

    /**
     * Delete the order by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Order : {}", id);
        orderRepository.deleteById(id);
    }
}
