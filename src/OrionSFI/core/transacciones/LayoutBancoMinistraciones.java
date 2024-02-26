/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *27/08/2015    4030                    Creación del proyecto 	Román
 */
package OrionSFI.core.transacciones;

import OrionSFI.core.commons.InspectorDatos;
import OrionSFI.core.commons.JDBCConnectionPool;
import OrionSFI.core.commons.MensajesSistema;
import OrionSFI.core.commons.SQLProperties;
import OrionSFI.core.institucion.Institucion;
import OrionSFI.core.reportesDEPP.ReportesDEPP;
import OrionSFI.reportes.Reporte;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Roman
 */
public class LayoutBancoMinistraciones {

	// <editor-fold defaultstate="collapsed" desc="validacionesIniciales">
	public List<String> generaDispersion(short numeroInstitucion, String VAL_TIPO_DISPOSICION,
			List<String> foliosOperacion, String usuario, String MacAddress) throws Exception {

		JDBCConnectionPool bd = null;
		ResultSet resultado;
		StringBuffer query = new StringBuffer();
		List<String> listDatosIniciales = new ArrayList<String>();
		SQLProperties sqlProperties = new SQLProperties();
		InspectorDatos iDat = new InspectorDatos();
		String separador = sqlProperties.getSeparador();
		SimpleDateFormat formatoSoloFechaOrion = new SimpleDateFormat(sqlProperties.getFormatoSoloFechaOrion());
		Institucion institucion = new Institucion();

		try {
			bd = new JDBCConnectionPool();
			if (foliosOperacion != null && foliosOperacion.size() > 0) {

				for (String folio : foliosOperacion) {
					System.out.println(folio);
					int utilizaConvenio = 0;
					String tipoLayoutBanco = "", numeroCuenta = "", numeroMinistracion = "", referenciaCIE = "",
							conceptoCIE = "", institucionBancariaOrigen = "", institucionBancariaDestino = "",
							montoTransaccion = "", motivoPago = "", titularCuentaBancaDestino = "", convenio = "",
							numeroCuentaDestino = "", cuentaBancariaOrigen = "";
					bd = new JDBCConnectionPool();
					query = new StringBuffer(" SELECT ");
					query.append("  CLBC.TIPO_LAYOUT_BANCO ");
					query.append(" ,RPDM.INSTITUCION_BANCARIA_DESTINO ");
					query.append(" ,RPDM.INSTITUCION_BANCARIA_ORIGEN ");
					query.append(" ,RPDM.NUMERO_CUENTA ");
					query.append(" ,RPDM.CUENTA_BANCARIA_DESTINO ");
					query.append(" ,RPDM.CUENTA_BANCARIA_ORIGEN ");
					query.append(" ,RPDM.NUMERO_MINISTRACION ");
					query.append(" ,RPDM.MONTO_TRANSACCION ");
					query.append(" ,RPDM.CONVENIO ");
					query.append(" ,RPDM.TITULAR_CUENTA_BANCARIA_DESTINO ");
					query.append(" FROM CRE_LAYOUT_BANCO_CONFIGURACION CLBC");
					query.append(" INNER JOIN REG_PROCESO_DISPOSICION_MASIVA RPDM ");
					query.append(" ON CLBC.NUMERO_INSTITUCION = RPDM.NUMERO_INSTITUCION");
					query.append(" AND CLBC.NUMERO_INSTITUCION_FINANCIERA = RPDM.INSTITUCION_BANCARIA_ORIGEN");
					query.append(" WHERE CLBC.NUMERO_INSTITUCION = ").append(numeroInstitucion);
					query.append(" AND RPDM.INSTRUMENTO_MONETARIO = 'T'");
					query.append(" AND RPDM.FOLIO_OPERACION = ").append(folio);
					query.append(" AND CLBC.STATUS = 1 ");
					System.out.println(query);
					resultado = bd.executeQuery(query.toString());
					if (resultado.next()) {
						tipoLayoutBanco = resultado.getString("TIPO_LAYOUT_BANCO");
						numeroCuenta = resultado.getString("NUMERO_CUENTA");
						numeroMinistracion = resultado.getString("NUMERO_MINISTRACION");
						montoTransaccion = resultado.getString("MONTO_TRANSACCION");
						titularCuentaBancaDestino = resultado.getString("TITULAR_CUENTA_BANCARIA_DESTINO");
						institucionBancariaOrigen = resultado.getString("INSTITUCION_BANCARIA_ORIGEN");
						institucionBancariaDestino = resultado.getString("INSTITUCION_BANCARIA_DESTINO");
						convenio = resultado.getString("CONVENIO");
						numeroCuentaDestino = resultado.getString("CUENTA_BANCARIA_DESTINO");
						cuentaBancariaOrigen = resultado.getString("CUENTA_BANCARIA_ORIGEN");
						if (VAL_TIPO_DISPOSICION.equals("MULTIPLE")) {
							query = new StringBuffer(" SELECT ");
							query.append(" 		UTILIZA_CONVENIO ");
							query.append(" 		,REFERENCIA_CIE ");
							query.append(" 		,CONCEPTO_CIE  ");
							query.append(" 		,MOTIVO_PAGO  ");
							query.append(" 	FROM CRE_CARGA_MINISTRACIONES ");
							query.append(" WHERE NUMERO_INSTITUCION = ").append(numeroInstitucion);
							query.append(" AND NUMERO_CUENTA = ").append(numeroCuenta);
							query.append(" AND NUMERO_TIPO_MINISTRACION = ").append(numeroMinistracion);
							query.append(" AND STATUS = 1 ");
							System.out.println(query);
							resultado = bd.executeQuery(query.toString());
							if (resultado.next()) {
								utilizaConvenio = resultado.getInt("UTILIZA_CONVENIO");
								referenciaCIE = resultado.getString("REFERENCIA_CIE");
								conceptoCIE = resultado.getString("CONCEPTO_CIE");
								motivoPago = resultado.getString("MOTIVO_PAGO");
							}
						}
						if (tipoLayoutBanco.equals("BBVA")) {
							if (institucionBancariaOrigen.equals(institucionBancariaDestino)) {
								if (VAL_TIPO_DISPOSICION.equals("MULTIPLE")) {
									if (utilizaConvenio == 1) {
										// INSERT LAYOUT_BBVA_CONVENIO
										if (motivoPago == "" || motivoPago == null)
											motivoPago = titularCuentaBancaDestino;
										query = new StringBuffer(insertLayoutBbbvaConvenio(numeroInstitucion,
												conceptoCIE, convenio, cuentaBancariaOrigen, montoTransaccion,
												motivoPago, referenciaCIE, usuario, MacAddress));
										System.out.println(query);
										bd.executeUpdate(query.toString());
									} else {
										// INSERT LAYOUT_BBVA_BANCARIOS
										if (motivoPago == "" || motivoPago == null)
											motivoPago = titularCuentaBancaDestino;
										query = new StringBuffer(insertLayoutBbbvaBancarios(numeroInstitucion,
												numeroCuentaDestino, cuentaBancariaOrigen, montoTransaccion, motivoPago,
												usuario, MacAddress));
										System.out.println(query);
										bd.executeUpdate(query.toString());
									}
								} else {
									// INSERT LAYOUT_BBVA_BANCARIOS
									if (motivoPago == "" || motivoPago == null)
										motivoPago = titularCuentaBancaDestino;
									query = new StringBuffer(insertLayoutBbbvaBancarios(numeroInstitucion,
											numeroCuentaDestino, cuentaBancariaOrigen, montoTransaccion, motivoPago,
											usuario, MacAddress));
									System.out.println(query);
									bd.executeUpdate(query.toString());
								}
							} else {
								// INSER LAYOUT_BBVA_INTERBANCARIOS
								if (motivoPago == "" || motivoPago == null)
									motivoPago = titularCuentaBancaDestino;
								query = new StringBuffer(insertLayoutBbbvaInterbancarios(numeroInstitucion,
										numeroCuentaDestino, cuentaBancariaOrigen, montoTransaccion,
										titularCuentaBancaDestino, institucionBancariaOrigen, motivoPago, referenciaCIE,
										usuario, MacAddress));
								System.out.println(query);
								bd.executeUpdate(query.toString());
							}
						} else
							continue;
					}

				} // TERMINA FOREACH
				System.out.println("-------------------creacion archivo CSV-------------------------");
				ReportesDEPP reportesDEPP = new ReportesDEPP();
				Map<String, String> hmp = new HashMap();
				Map<String, String> hmpFiltros = new HashMap();
				Map<String, String> hmpList = new HashMap();
				Map<String, String> hmpInfoGeoUsr = new HashMap<String, String>();

				Date ldtActual = new Date();
				String cadenaHora = (new SimpleDateFormat(sqlProperties.getformatoReporteHora())).format(ldtActual);
				Date date = new Date();
				SimpleDateFormat formato_hora = new SimpleDateFormat(sqlProperties.getformatoReporteHora());
				cadenaHora = formato_hora.format(date);
				String cadenadia = new SimpleDateFormat("yyyyMMdd").format(date);
				String lstNombreArchivoFinalConvenio = "REPORTE_PAGOS_TRANSFERENCIAS_CIE_BBVA" + "_" + cadenadia + "_" + cadenaHora + ".txt";
				String lstNombreArchivoFinalBancario = "REPORTE_PAGOS_TRANSFERENCIAS_BANCARIAS_BBVA" + "_" + cadenadia + "_" + cadenaHora + ".txt";
				String lstNombreArchivoFinalInterbancario = "REPORTE_PAGOS_TRANSFERENCIAS_INTERBANCARIAS_BBVA" + "_" + cadenadia + "_" + cadenaHora + ".txt";
				hmp.put("SERIAL_NUMBER", Short.toString(numeroInstitucion));
				hmp.put("ID_USUARIO", usuario);
				hmp.put("CLAVE_FUNCION", lstNombreArchivoFinalConvenio);
				hmp.put("TIPO_REPORTE", "0");
				// hmp.put("TIPO_REPORTE", "3");
				Reporte reporte = new Reporte(hmp, hmpFiltros, hmpList, "");

				reporte.creaCarpeta();
				String path = reporte.getPathReporte();
				System.err.println(path);
				List<String> lisTxt = new ArrayList<String>();
				
				lisTxt = getCsvConvenio(numeroInstitucion);
				if(lisTxt.size()>0 && lisTxt != null)
					FileUtils.writeLines(new File(path + File.separator + lstNombreArchivoFinalConvenio), "ISO-8859-1", lisTxt);
				lisTxt = getCsvBancario(numeroInstitucion);
				if(lisTxt.size()>0 && lisTxt != null)
					FileUtils.writeLines(new File(path + File.separator + lstNombreArchivoFinalBancario), "ISO-8859-1", lisTxt);
				lisTxt = getCsvInterbancario(numeroInstitucion);
				if(lisTxt.size()>0 && lisTxt != null)
					FileUtils.writeLines(new File(path + File.separator + lstNombreArchivoFinalInterbancario), "ISO-8859-1", lisTxt);
				
				//borra tablas temporales de reportes
				query = (new StringBuffer("DELETE FROM CRE_LAYOUT_BBVA_CONVENIO"));
				query.append(" WHERE NUMERO_INSTITUCION=").append(numeroInstitucion);
				System.out.println(query);
				bd.executeUpdate(query.toString());
				query = (new StringBuffer("DELETE FROM CRE_LAYOUT_BBVA_INTERBANCARIOS"));
				query.append(" WHERE NUMERO_INSTITUCION=").append(numeroInstitucion);
				System.out.println(query);
				bd.executeUpdate(query.toString());
				query = (new StringBuffer("DELETE FROM CRE_LAYOUT_BBVA_BANCARIOS"));
				query.append(" WHERE NUMERO_INSTITUCION=").append(numeroInstitucion);
				System.out.println(query);
				bd.executeUpdate(query.toString());
			}

			bd.close();
		} catch (SQLException se) {
			// libera la conexion
			bd.close();
			se.printStackTrace();
			throw new SQLException("131");
		}

		return listDatosIniciales;
	}

	private List<String> getCsvConvenio(short numeroInstitucion) throws Exception {

		StringBuffer query = new StringBuffer();
		SQLProperties sqlProperties = new SQLProperties();
		List<String> liCsv = new ArrayList<String>();
		JDBCConnectionPool bd = new JDBCConnectionPool();
		try {
			query = new StringBuffer();
			query.append(" SELECT ");
			query.append(" CONCEPTO_CIE ");
			query.append(" ,CONVENIO ");
			query.append(" ,ASUNTO_ORDENANTE ");
			query.append(" ,IMPORTE_OPERACION ");
			query.append(" ,MOTIVO_PAGO ");
			query.append(" ,CLAVE_ALEATORIA ");
			query.append(" ,REFRENCIA_CIE ");
			query.append(" FROM dbo.CRE_LAYOUT_BBVA_CONVENIO ");
			query.append("WHERE NUMERO_INSTITUCION = ").append(numeroInstitucion);

			System.out.println(query);
			ResultSet resultado = bd.executeQuery(query.toString());

			// liCsv = sqlProperties.getColumnName(resultado, ",");
			liCsv = sqlProperties.getColumnValueFormatoFecha(resultado, liCsv, sqlProperties.getFormatoSoloFechaOrion(),
					"");
		} catch (Exception se) {
			// reportesDEPP.borraTabla(numeroInstitucion, this.usuarioReporte,
			// this.fechaEmision, "REP_953_DETALLE", this.bd);
			bd.close();
			se.printStackTrace();
			MensajesSistema mensajesSistema = new MensajesSistema();
			throw new SQLException(mensajesSistema.getMensaje(131));
		}
		return liCsv;
	}

	private List<String> getCsvInterbancario(short numeroInstitucion) throws Exception {

		StringBuffer query = new StringBuffer();
		SQLProperties sqlProperties = new SQLProperties();
		List<String> liCsv = new ArrayList<String>();
		JDBCConnectionPool bd = new JDBCConnectionPool();
		try {
			query = new StringBuffer();
			query.append(" SELECT ");
			query.append(" ASUNTO_BENEFICIARIO ");
			query.append(" ,ASUNTO_ORDENANTE ");
			query.append(" ,DIVISA ");
			query.append(" ,IMPORTE ");
			query.append(" ,TITULAR_CUENTA_BANCARIA_DESTINO ");
			query.append(" ,TIPO_CUENTA ");
			query.append(" ,INSTITUCION_BANCARIA_DESTINO ");
			query.append(" ,MOTIVO_PAGO ");
			query.append(" ,CLAVE_ALEATORIA ");
			query.append(" ,REFRENCIA_NUMERICA ");
			query.append(" ,DISPONIBILIDAD ");
			query.append(" FROM CRE_LAYOUT_BBVA_INTERBANCARIOS ");
			query.append("WHERE NUMERO_INSTITUCION = ").append(numeroInstitucion);

			System.out.println(query);
			ResultSet resultado = bd.executeQuery(query.toString());

			// liCsv = sqlProperties.getColumnName(resultado, ",");
			liCsv = sqlProperties.getColumnValueFormatoFecha(resultado, liCsv, sqlProperties.getFormatoSoloFechaOrion(),
					"");
		} catch (Exception se) {
			// reportesDEPP.borraTabla(numeroInstitucion, this.usuarioReporte,
			// this.fechaEmision, "REP_953_DETALLE", this.bd);
			bd.close();
			se.printStackTrace();
			MensajesSistema mensajesSistema = new MensajesSistema();
			throw new SQLException(mensajesSistema.getMensaje(131));
		}
		return liCsv;
	}
	
	private List<String> getCsvBancario(short numeroInstitucion) throws Exception {

		StringBuffer query = new StringBuffer();
		SQLProperties sqlProperties = new SQLProperties();
		List<String> liCsv = new ArrayList<String>();
		JDBCConnectionPool bd = new JDBCConnectionPool();
		try {
			query = new StringBuffer();
			query.append(" SELECT ");
			query.append(" ASUNTO_BENEFICIARIO ");
			query.append(" ,ASUNTO_ORDENANTE ");
			query.append(" ,DIVISA ");
			query.append(" ,IMPORTE ");
			query.append(" ,MOTIVO_PAGO ");
			query.append(" ,CLAVE_ALEATORIA ");
			query.append(" FROM CRE_LAYOUT_BBVA_INTERBANCARIOS ");
			query.append("WHERE NUMERO_INSTITUCION = ").append(numeroInstitucion);

			System.out.println(query);
			ResultSet resultado = bd.executeQuery(query.toString());

			// liCsv = sqlProperties.getColumnName(resultado, ",");
			liCsv = sqlProperties.getColumnValueFormatoFecha(resultado, liCsv, sqlProperties.getFormatoSoloFechaOrion(),
					"");
		} catch (Exception se) {
			// reportesDEPP.borraTabla(numeroInstitucion, this.usuarioReporte,
			// this.fechaEmision, "REP_953_DETALLE", this.bd);
			bd.close();
			se.printStackTrace();
			MensajesSistema mensajesSistema = new MensajesSistema();
			throw new SQLException(mensajesSistema.getMensaje(131));
		}
		return liCsv;
	}

	private int generateRandom7DigitNumber() {
		// Creamos una instancia de la clase Random
		Random random = new Random();

		// Generamos un número aleatorio de 7 dígitos (entre 1000000 y 9999999)
		int randomNumber = 1000000 + random.nextInt(9000000);

		return randomNumber;
	}

	private StringBuffer insertLayoutBbbvaInterbancarios(short numeroInstitucion, String numeroCuentaDestino,
			String cuentaBancariaOrigen, String importe, String titularCuentaBancaDestino,
			String institucionBancariaOrigen, String motivoPago, String referencia, String usuario, String MacAdress) {
		StringBuffer query = new StringBuffer();
		query = new StringBuffer(" INSERT ");
		query.append(" INTO CRE_LAYOUT_BBVA_INTERBANCARIOS");
		query.append(" (NUMERO_INSTITUCION ");

		query.append(" ,ASUNTO_BENEFICIARIO ");
		query.append(" ,ASUNTO_ORDENANTE ");
		query.append(" ,DIVISA ");
		query.append(" ,IMPORTE ");
		query.append(" ,TITULAR_CUENTA_BANCARIA_DESTINO ");
		query.append(" ,TIPO_CUENTA ");
		query.append(" ,INSTITUCION_BANCARIA_DESTINO ");
		query.append(" ,MOTIVO_PAGO ");
		query.append(" ,CLAVE_ALEATORIA ");
		query.append(" ,REFRENCIA_NUMERICA ");
		query.append(" ,DISPONIBILIDAD ");
		query.append(" ,STATUS ");
		query.append(" ,FECHA_ALTA ");
		query.append(" ,ID_USUARIO_ALTA ");
		query.append(" ,MAC_ADDRESS_ALTA) ");
		query.append(" VALUES ");
		query.append(" ( ").append(numeroInstitucion);
		query.append(",'").append(ajustarLongitudConCeros(numeroCuentaDestino, 18));
		query.append("',").append(ajustarLongitudConCeros(cuentaBancariaOrigen, 18));
		query.append(",'MXP' ");// DIVISA
		query.append(",").append(ajustarLongitudConCeros(importe, 16));
		query.append(",'").append(ajustarLongitud(titularCuentaBancaDestino, 30)).append("'");
		query.append(",'40' ");// TIPO_CUENTA
		query.append(",").append(ajustarLongitudConCeros(institucionBancariaOrigen, 3));
		query.append(",'").append(ajustarLongitudConCeros(motivoPago, 23));
		query.append("',").append(generateRandom7DigitNumber());// numeros random
		query.append(",'").append(ajustarLongitudConCeros(referencia, 7));
		query.append("','H' ");
		query.append(",1 ");
		query.append(",GETDATE() ");
		query.append(",'").append(usuario).append("' ");
		query.append(",'").append(MacAdress).append("' ");
		query.append(" ) ");
		return query;
	}

	private StringBuffer insertLayoutBbbvaConvenio(short numeroInstitucion, String conceptoCIE, String convenio,
			String cuentaBancaOrigen, String montoTransaccion, String motivoPago, String referenciaCIE, Object usuario,
			String MacAdress) {
		StringBuffer query = new StringBuffer();
		query = new StringBuffer(" INSERT ");
		query.append(" INTO CRE_LAYOUT_BBVA_CONVENIO");
		query.append(" (NUMERO_INSTITUCION ");
		query.append(" ,CONCEPTO_CIE ");
		query.append(" ,CONVENIO ");
		query.append(" ,ASUNTO_ORDENANTE ");
		query.append(" ,IMPORTE_OPERACION ");
		query.append(" ,MOTIVO_PAGO ");
		query.append(" ,CLAVE_ALEATORIA ");
		query.append(" ,REFRENCIA_CIE ");
		query.append(" ,STATUS ");
		query.append(" ,FECHA_ALTA ");
		query.append(" ,ID_USUARIO_ALTA ");
		query.append(" ,MAC_ADDRESS_ALTA) ");
		query.append(" VALUES ");
		query.append(" ( ").append(numeroInstitucion);
		query.append(",'").append(ajustarLongitud(conceptoCIE, 30));
		query.append("',").append(ajustarLongitudConCeros(convenio, 7));
		query.append(",").append(ajustarLongitudConCeros(cuentaBancaOrigen, 18));
		query.append(",").append(ajustarLongitudConCeros(montoTransaccion, 16));
		query.append(",'").append(ajustarLongitudConCeros(motivoPago, 23));
		query.append("',").append(generateRandom7DigitNumber());// numeros random
		query.append(",'").append(ajustarLongitud(referenciaCIE, 20)).append("'");
		query.append(",1 ");
		query.append(",GETDATE() ");
		query.append(",'").append(usuario).append("' ");
		query.append(",'").append(MacAdress).append("' ");
		query.append(" ) ");
		return query;
	}

	private StringBuffer insertLayoutBbbvaBancarios(short numeroInstitucion, String numeroCuentaDestino,
			String cuentaBancariaOrigen, String importe, String motivoPago, String usuario, String MacAdress) {
		StringBuffer query = new StringBuffer();
		query = new StringBuffer(" INSERT ");
		query.append(" INTO CRE_LAYOUT_BBVA_BANCARIOS");
		query.append(" (NUMERO_INSTITUCION ");
		query.append(" ,ASUNTO_BENEFICIARIO");
		query.append(" ,ASUNTO_ORDENANTE");
		query.append(" ,DIVISA");
		query.append(" ,IMPORTE");
		query.append(" ,MOTIVO_PAGO");
		query.append(" ,CLAVE_ALEATORIA");
		query.append(" ,STATUS ");
		query.append(" ,FECHA_ALTA ");
		query.append(" ,ID_USUARIO_ALTA ");
		query.append(" ,MAC_ADDRESS_ALTA) ");
		query.append(" VALUES ");
		query.append(" ( ").append(numeroInstitucion);
		query.append(",'").append(ajustarLongitudConCeros(numeroCuentaDestino, 18));
		query.append("',").append(ajustarLongitudConCeros(cuentaBancariaOrigen, 18));
		query.append(",'MXP' ");// DIVISA
		query.append(",").append(ajustarLongitudConCeros(importe, 16));
		query.append(",'").append(ajustarLongitudConCeros(motivoPago, 23));
		query.append("','").append(generateRandom7DigitNumber());// numeros random
		query.append("',1 ");
		query.append(",GETDATE() ");
		query.append(",'").append(usuario).append("' ");
		query.append(",'").append(MacAdress).append("' ");
		query.append(" ) ");
		return query;
	}

	private static String ajustarLongitud(String cadena, int longitudObjetivo) {
		// Truncar la cadena si es más larga que la longitud objetivo
		if (cadena.length() > longitudObjetivo) {
			return cadena.substring(0, longitudObjetivo);
		}

		// Completar con espacios a la derecha si es menor que la longitud objetivo
		StringBuilder cadenaAjustada = new StringBuilder(cadena);
		while (cadenaAjustada.length() < longitudObjetivo) {
			cadenaAjustada.append(" ");
		}

		return cadenaAjustada.toString();
	}

	private static String ajustarLongitudConCeros(String cadena, int longitudObjetivo) {
		// Truncar la cadena si es más larga que la longitud objetivo
		if (cadena.length() > longitudObjetivo) {
			return cadena.substring(0, longitudObjetivo);
		}

		// Completar con ceros a la izquierda si es menor que la longitud objetivo
		StringBuilder cadenaAjustada = new StringBuilder();
		int cerosParaAgregar = longitudObjetivo - cadena.length();
		for (int i = 0; i < cerosParaAgregar; i++) {
			cadenaAjustada.append("0");
		}
		cadenaAjustada.append(cadena);

		return cadenaAjustada.toString();
	}

	public LayoutBancoMinistraciones() {

	}

	public static void main(String[] args) throws Exception {
		short numeroInstitucion = 1;
		String idUsuario = "USRCONFIG";
		String macAddresS = "00:00:00:00";
		List<String> folios = new ArrayList<>();
		folios.add("123123");

		LayoutBancoMinistraciones obj = new LayoutBancoMinistraciones();
		System.out.println("." + obj.ajustarLongitud("HOLA NO AAAAAAME LLAMO BRANDON", 30) + ".");
		System.out.println("." + obj.ajustarLongitudConCeros("163466019.75", 18) + ".");
		obj.generaDispersion(numeroInstitucion, "MULTIPLE", folios, idUsuario, macAddresS);
		// obj.validacionesIniciales((short)1, "MULTIPLE", null);
		// obj.validaExpedienteCliente((short)1, 10, 561);
		// obj.ejecutaTransaccion(numeroInstitucion, transaccionExterna,
		// fechaAplicacion, numeroCuentaOrigen, formaDisposicion, montoDisposicion,
		// tipoCuentaEje, numCuentaEje, sistemaOperacion, sistemaOrigen,
		// folioAutorizacion, referenciaNumerica, referenciaAlfanumerica,
		// tipoDisposicion, idUsuario, macAddresS);
	}
}
