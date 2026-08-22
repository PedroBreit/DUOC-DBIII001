package cl.duoc.bancobatch.model;

import java.math.BigDecimal;

public class EstadoCuentaAnual {

    private Long cuentaId;
    private BigDecimal totalDepositos;
    private BigDecimal totalRetiros;
    private BigDecimal totalCompras;
    private BigDecimal saldoNeto;
    private Integer cantidadMovimientos;

    public EstadoCuentaAnual() {
    }

    public EstadoCuentaAnual(
            Long cuentaId,
            BigDecimal totalDepositos,
            BigDecimal totalRetiros,
            BigDecimal totalCompras,
            BigDecimal saldoNeto,
            Integer cantidadMovimientos) {

        this.cuentaId = cuentaId;
        this.totalDepositos = totalDepositos;
        this.totalRetiros = totalRetiros;
        this.totalCompras = totalCompras;
        this.saldoNeto = saldoNeto;
        this.cantidadMovimientos = cantidadMovimientos;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public BigDecimal getTotalDepositos() {
        return totalDepositos;
    }

    public void setTotalDepositos(BigDecimal totalDepositos) {
        this.totalDepositos = totalDepositos;
    }

    public BigDecimal getTotalRetiros() {
        return totalRetiros;
    }

    public void setTotalRetiros(BigDecimal totalRetiros) {
        this.totalRetiros = totalRetiros;
    }

    public BigDecimal getTotalCompras() {
        return totalCompras;
    }

    public void setTotalCompras(BigDecimal totalCompras) {
        this.totalCompras = totalCompras;
    }

    public BigDecimal getSaldoNeto() {
        return saldoNeto;
    }

    public void setSaldoNeto(BigDecimal saldoNeto) {
        this.saldoNeto = saldoNeto;
    }

    public Integer getCantidadMovimientos() {
        return cantidadMovimientos;
    }

    public void setCantidadMovimientos(Integer cantidadMovimientos) {
        this.cantidadMovimientos = cantidadMovimientos;
    }

    @Override
    public String toString() {
        return "EstadoCuentaAnual{" +
                "cuentaId=" + cuentaId +
                ", totalDepositos=" + totalDepositos +
                ", totalRetiros=" + totalRetiros +
                ", totalCompras=" + totalCompras +
                ", saldoNeto=" + saldoNeto +
                ", cantidadMovimientos=" + cantidadMovimientos +
                '}';
    }
}