package cl.duoc.bancobatch.model;

import java.math.BigDecimal;

public class InteresCalculado {

    private Long cuentaId;
    private String nombre;
    private BigDecimal saldoInicial;
    private Integer edad;
    private String tipo;
    private BigDecimal tasa;
    private BigDecimal interes;
    private BigDecimal saldoFinal;

    public InteresCalculado() {
    }

    public InteresCalculado(
            Long cuentaId,
            String nombre,
            BigDecimal saldoInicial,
            Integer edad,
            String tipo,
            BigDecimal tasa,
            BigDecimal interes,
            BigDecimal saldoFinal) {

        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldoInicial = saldoInicial;
        this.edad = edad;
        this.tipo = tipo;
        this.tasa = tasa;
        this.interes = interes;
        this.saldoFinal = saldoFinal;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getTasa() {
        return tasa;
    }

    public void setTasa(BigDecimal tasa) {
        this.tasa = tasa;
    }

    public BigDecimal getInteres() {
        return interes;
    }

    public void setInteres(BigDecimal interes) {
        this.interes = interes;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    @Override
    public String toString() {
        return "InteresCalculado{" +
                "cuentaId=" + cuentaId +
                ", nombre='" + nombre + '\'' +
                ", saldoInicial=" + saldoInicial +
                ", edad=" + edad +
                ", tipo='" + tipo + '\'' +
                ", tasa=" + tasa +
                ", interes=" + interes +
                ", saldoFinal=" + saldoFinal +
                '}';
    }
}
