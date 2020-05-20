package com.reljicd.service.impl;

import com.reljicd.exception.NotEnoughProductsInStockException;
import com.reljicd.model.Product;

import com.reljicd.repository.ProductRepository;

import com.reljicd.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shopping Cart is implemented with a Map, and as a session bean
 *
 * @author Dusan
 */
@Service
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ProductRepository productRepository;
    
	/*
	 * @Autowired private final StoreRepository storeRepository= null ;
	 */
    
    

    private Map<Product, Integer> products = new HashMap<>();
    
    String name=null;
    BigDecimal price;
    @Autowired
    JdbcTemplate jt;

    @Autowired
    public ShoppingCartServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * If product is in the map just increment quantity by 1.
     * If product is not in the map with, add it with quantity 1
     *
     * @param product
     */
    @Override
    public void addProduct(Product product) {
        if (products.containsKey(product)) {
            products.replace(product, products.get(product) + 1);
        } else {
            products.put(product, 1);
        }
    }

    /**
     * If product is in the map with quantity > 1, just decrement quantity by 1.
     * If product is in the map with quantity 1, remove it from map
     *
     * @param product
     */
    @Override
    public void removeProduct(Product product) {
        if (products.containsKey(product)) {
            if (products.get(product) > 1)
                products.replace(product, products.get(product) - 1);
            else if (products.get(product) == 1) {
                products.remove(product);
            }
        }
    }

    /**
     * @return unmodifiable copy of the map
     */
    @Override
    public Map<Product, Integer> getProductsInCart() {
        return Collections.unmodifiableMap(products);
    }

    /**
     * Checkout will rollback if there is not enough of some product in stock
     *
     * @throws NotEnoughProductsInStockException
     */
    @Override
    public void checkout() throws NotEnoughProductsInStockException {
        Product product;
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            // Refresh quantity for every product before checking
            product = productRepository.findOne(entry.getKey().getId());
            
           
            if (product.getQuantity() < entry.getValue())
                throw new NotEnoughProductsInStockException(product);
            entry.getKey().setQuantity(product.getQuantity() - entry.getValue());
            
          name= entry.getKey().getName();
          
          price= entry.getKey().getPrice();
          
          System.out.println("==========name====="+name);
          System.out.println("==========name====="+price);
        }
        
      //  storeRepository.save(new Stores("w","ww",$200));
        
      //  storeRepository.save1(new Stores("w","ww",BigDecimal.valueOf(299)));
        
       // storeRepository.saveAndFlush(new Stores("w","ww",$200));
        
        
        
        
        
       String insert_sql="insert into stores values(?,?,?)";
		
		int rows=jt.update(new PreparedStatementCreator() {
			
			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException
			
			{
				PreparedStatement pst=con.prepareStatement(insert_sql);
				
				
				pst.setString(1, "admins");
				pst.setString(2,name);
				pst.setBigDecimal(3, price);
				
				
				return pst;
			}

			
		});
        
        productRepository.save(products.keySet());
        productRepository.flush();
        
        products.entrySet().stream()
		.map(entry -> entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())))
        .reduce(BigDecimal::add)
        .orElse(BigDecimal.ZERO);

       // products.clear();
    }

    @Override
    public BigDecimal getTotal() {
        return products.entrySet().stream()
        		.map(entry -> entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        
        
      //  System.out.println("==+++"+entry ->entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
    }
}
