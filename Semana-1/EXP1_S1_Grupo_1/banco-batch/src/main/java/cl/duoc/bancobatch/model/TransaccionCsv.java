package cl.duoc.bancobatch.model;

import java.math.BigDecimal;

public class TransaccionCsv {

    private Long id;
    private String fecha;
    private BigDecimal monto;
    private String tipo;

    public TransaccionCsv() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "TransaccionCsv{" +
                "id=" + id +
                ", fecha='" + fecha + '\'' +
                ", monto=" + monto +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}
