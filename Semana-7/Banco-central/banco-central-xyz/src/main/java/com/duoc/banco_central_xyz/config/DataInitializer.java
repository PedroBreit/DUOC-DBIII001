package com.duoc.banco_central_xyz.config;

import com.duoc.banco_central_xyz.entity.CuentaBancaria;
import com.duoc.banco_central_xyz.repository.CuentaBancariaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner inicializarCuentaDemo(
            CuentaBancariaRepository cuentaBancariaRepository
    ) {

        return args -> {

            /*
             * La cuenta se crea solamente si no existe.
             * De esta manera los retiros realizados durante
             * las pruebas no se pierden al reiniciar el servicio.
             */
            if (cuentaBancariaRepository
                    .findByCuentaIdLegacy(137L)
                    .isPresent()) {

                return;
            }

            CuentaBancaria cuenta = new CuentaBancaria();

            cuenta.setCuentaIdLegacy(137L);
            cuenta.setNombre("Steve Rogers");

            cuenta.setSaldo(
                    new BigDecimal("10000.00")
            );

            cuenta.setEdad(25);

            cuenta.setTipo("ahorro");

            cuenta.setInteres(
                    new BigDecimal("500.00")
            );

            cuenta.setSaldoFinal(
                    new BigDecimal("10500.00")
            );

            cuentaBancariaRepository.save(cuenta);

            System.out.println(
                    "Cuenta demo legacy 137 inicializada correctamente"
            );
        };
    }
}