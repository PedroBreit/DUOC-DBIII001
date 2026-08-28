package cl.duoc.bancobatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ResumenTransaccionDiaria {

    private LocalDate fecha;
    private Integer cantidadTransacciones;
    private BigDecimal totalDebitos;
    private BigDecimal totalCreditos;
    private BigDecimal montoTotal;

    public ResumenTransaccionDiaria() {
    }

    public ResumenTransaccionDiaria(
            LocalDate fecha,
            Integer cantidadTransacciones,
            BigDecimal totalDebitos,
            BigDecimal totalCreditos,
            BigDecimal montoTotal) {

        this.fecha = fecha;
        this.cantidadTransacciones = cantidadTransacciones;
        this.totalDebitos = totalDebitos;
        this.totalCreditos = totalCreditos;
        this.montoTotal = montoTotal;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Integer getCantidadTransacciones() {
        return cantidadTransacciones;
    }

    public void setCantidadTransacciones(Integer cantidadTransacciones) {
        this.cantidadTransacciones = cantidadTransacciones;
    }

    public BigDecimal getTotalDebitos() {
        return totalDebitos;
    }

    public void setTotalDebitos(BigDecimal totalDebitos) {
        this.totalDebitos = totalDebitos;
    }

    public BigDecimal getTotalCreditos() {
        return totalCreditos;
    }

    public void setTotalCreditos(BigDecimal totalCreditos) {
        this.totalCreditos = totalCreditos;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    @Override
    public String toString() {
        return "ResumenTransaccionDiaria{" +
                "fecha=" + fecha +
                ", cantidadTransacciones=" + cantidadTransacciones +
                ", totalDebitos=" + totalDebitos +
                ", totalCreditos=" + totalCreditos +
                ", montoTotal=" + montoTotal +
                '}';
    }
}
