
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

class ErrorHandler extends DefaultHandler {
	private ArrayList<String> warnings = new ArrayList<String>();
	private ArrayList<String> error = new ArrayList<String>();
	private ArrayList<String> fatal = new ArrayList<String>();
	private boolean e = false;

	public ErrorHandler() {
	}

	public void warnings(SAXParseException spe) {
		warnings.add(spe.toString());
		e = true;
		
	}

	public void error(SAXParseException spe) {
		error.add(spe.toString());
		e = true;
		
	}

	public void fatalerror(SAXParseException spe) {
		fatal.add(spe.toString());
		e = true;
	
	}

	public void formError(String spe) {
		fatal.add(spe);
		e = true;
	}

	public boolean getE() {
		return e;
	}

	public ArrayList<String> getWarnings() {
		return warnings;
	}

	public ArrayList<String> getError() {
		return error;
	}

	public ArrayList<String> getFatalError() {
		return fatal;
	}
}

public class Servlet extends HttpServlet {

	public void Validar(String xml, HashMap<String,ErrorHandler> incorrectos, HashMap<String, Element> correctos,URL schema_loc)
			throws ParserConfigurationException, SAXException, IOException {

		Source xmlFile = new StreamSource(xml);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(schema_loc);
		Validator validator = schema.newValidator();

		ErrorHandler err = new ErrorHandler();
		validator.setErrorHandler(err);
		
		String[] parts=xml.split("/");
		String fichero=parts[parts.length-1];
		System.out.println(fichero);

		try {
			validator.validate(xmlFile);
			
		}

		catch (Exception e) {
			
			err.formError(e.toString());
			

		}
		if (err.getE() == true) {
			if(!incorrectos.containsKey(fichero)){
				incorrectos.put(fichero, err);}

		} else {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xml); 
			Element titulacion = doc.getDocumentElement();
			if(!correctos.containsKey(titulacion.getElementsByTagName("NombreTit").item(0).getTextContent())){
				System.out.println("not repe");
				System.out.println(fichero);
			correctos.put(titulacion.getElementsByTagName("NombreTit").item(0).getTextContent(), titulacion);
			}
		}

	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		
		HashMap<String, Element> xmlCorrectos = new HashMap<String, Element>();
		HashMap<String,ErrorHandler> xmlIncorrectos = new  HashMap<String,ErrorHandler>();
		
		URL serv_url=new URL(req.getRequestURL().toString());
		URL schema_loc= new URL(serv_url,"eaml.xsd");
		
		URL css_loc= new URL(serv_url,"eaml.css");
		
		
		try {
	//		Validar("teleco.xml", xmlIncorrectos, xmlCorrectos,schema_loc);

			Validar("http://gssi.det.uvigo.es/users/agil/public_html/SINT/16-17/p2/teleco.xml", xmlIncorrectos, xmlCorrectos,schema_loc);
		} catch (Exception e) {
		}
		
		while(true){
			int listco=xmlCorrectos.size();
			int listinco=xmlIncorrectos.size();
			
		for (Element value:xmlCorrectos.values()){
			Document doc =value.getOwnerDocument();
			String[] parts=doc.getDocumentURI().split("/");
			String url="";
			for(int j=0;j<parts.length-1;j++){
				url=url+parts[j]+"/";
			}
		
		NodeList nl = value.getElementsByTagName("EAML");
		for (int i=0;i<nl.getLength();i++){
		try {
			if (nl.item(i).getTextContent().startsWith("http:")){
				Validar(nl.item(i).getTextContent(),xmlIncorrectos,xmlCorrectos,schema_loc);
			}
			else{
				Validar(url+nl.item(i).getTextContent(),xmlIncorrectos, xmlCorrectos,schema_loc);
			}			
		} catch (Exception e) {}
		}
		}
			if(listco==xmlCorrectos.size() && listinco==xmlIncorrectos.size()){
				break;
			}
		}
		

		int fase;
		String errores;
		String auto;
		if (req.getParameter("errores") == null) {
			errores = "n";
		} else {
			errores = req.getParameter("errores");
		}
		if (req.getParameter("auto") == null) {
			auto = "n";
		} else {
			auto = req.getParameter("auto");
		}
		if (req.getParameter("fase") == null) {
			fase = 0;
		} else {
			fase = Integer.parseInt(req.getParameter("fase"));
		}
		String titulacion = req.getParameter("titulacion");
		String materia = req.getParameter("materia");
		String alumno = req.getParameter("alumno");

		if (!auto.equals("si")) {
			res.setContentType("text/html");
		}

		PrintWriter out = res.getWriter();

		if (errores.equals("si")) {
			if (auto.equals("si")) {
				res.setCharacterEncoding("iso-8859-15");
				out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
				out.println("<errores>");
				out.println("<warnings>");
				
				for(String key : xmlIncorrectos.keySet()){
					if(xmlIncorrectos.get(key).getWarnings().size()>0){				
					for (String s : xmlIncorrectos.get(key).getWarnings()) {
						out.print("<warning>");					
						out.print("<file>"+key+"</file>");
						out.println("<cause>");
						out.println(s);
						out.println("</cause>");
						out.println("</warning>");
					}
					}
				}
				out.println("</warnings>");
				out.println("<errors>");
				
				for(String key : xmlIncorrectos.keySet()){
					if(xmlIncorrectos.get(key).getError().size()>0){
									
					for (String s : xmlIncorrectos.get(key).getError()) {
						out.print("<error>");
						out.print("<file>"+key+"</file>");
						out.println("<cause>");
						out.println(s);
						out.println("</cause>");
						out.println("</error>");
					}
					}
				}
				out.println("</errors>");
				out.println("<fatalerrors>");
				
				for(String key : xmlIncorrectos.keySet()){
					if(xmlIncorrectos.get(key).getFatalError().size()>0){
									
					for (String s : xmlIncorrectos.get(key).getFatalError()) {
						out.print("<fatalerror>");
						out.print("<file>"+key+"</file>");
						out.println("<cause>");
						out.println(s);
						out.println("</cause>");
						out.println("</fatalerror>");
					}
					}
				}
				out.println("</fatalerrors>");
				out.println("</errores>");
				
				
			} else {
				out.println("<html>");
				out.println("<head>");
				out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
				out.println("<title>Errores</title>");
				out.println("</head>");
				out.println("<body>");
				out.println("<form name='formulario2'>");
				out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
				int x=0;
				for(ErrorHandler value : xmlIncorrectos.values()){
					
					if(value.getWarnings().size()!=0){
						x++;
					}
				}
				
				out.println("<h3>Se han encontrado "+x+" ficheros con warnings:</h3><br>");
				
				for(ErrorHandler value : xmlIncorrectos.values()){
					for (String s : value.getWarnings()) {
						out.println("<li>"+s+"</li>");
					}
				}
				
				
				
				int z=0;
				for(ErrorHandler value : xmlIncorrectos.values()){
					
					if(value.getError().size()!=0){
						z++;
					}
				}
				
				out.println("<h3>Se han encontrado "+z+" ficheros con errores:</h3><br>");
				
				for(ErrorHandler value : xmlIncorrectos.values()){
					for (String s : value.getError()) {
						out.println("<li>"+s+"</li>");
					}
				}
				
				int k=0;
				for(ErrorHandler value : xmlIncorrectos.values()){
					
					if(value.getFatalError().size()!=0){
						k++;
					}
				}
				
				out.println("<h3>Se han encontrado "+k+" ficheros con errores fatales:</h3><br>");
				
				for(ErrorHandler value : xmlIncorrectos.values()){
					for (String s : value.getFatalError()) {
						out.println("<li>"+s+"</li>");
					}
				}

				out.println("<br><br><input class='button' type='submit' value='Atras'>");
				out.println("<input type='hidden' name='fase' value='0'>");
				out.println("</form>");
				out.println("<span>Javier García Rodríguez</span>");
				out.println("</body>");
				out.println("</html>");
			}
		}

		else {
			switch (fase) {

			case 0:
				if (auto.equals("si")) {
					res.setCharacterEncoding("iso-8859-15");
					out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
					out.println("<service>");
					out.println("<status>OK</status>");
					out.println("</service>");

					break;
				} else {
					out.println("<html>");
					out.println("<head>");
					out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
					out.println("<title>Fase_0</title>");
					out.println("</head>");
					out.println("<body>");
					out.println("<form name='formulario'>");
					out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
					out.println("<h2>Bienvenido a este servicio</h2><br>");
					out.println("<a href='?errores=si'>Pulsa aquí para ver los ficheros erróneos encontrados</a><br>");
					out.println("<h3>Selecciona una consulta:</h3><br>");
					out.println(
							"<input type='radio' name='info' id='info' value='info' checked='true'>Información sobre el alumno<br><br>");
					out.println("<input class='button' type='submit' value='Enviar'>");
					out.println("<input type='hidden' name='fase' value='1'>");
					out.println("</form>");
					out.println("<span>Javier García Rodríguez</span>");
					out.println("</body>");
					out.println("</html>");
					break;
				}
			case 1:
				if (auto.equals("si")) {
					res.setCharacterEncoding("iso-8859-15");
					out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
					out.println("<titulaciones>");
					ArrayList<String> tit = dataFase1(xmlCorrectos);
					for (int i = 0; i < tit.size(); i++) {
						out.println("<titulacion>" + tit.get(i) + "</titulacion>");
					}
					out.println("</titulaciones>");
					break;
				} else {
					out.println("<html>");
					out.println("<head>");
					out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
					out.println("<title>Fase_1</title>");
					out.println("</head>");
					out.println("<body>");
					out.println("<form name='formulario'>");
					out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
					out.println("<h3>Selecciona una titulación:</h3><br>");
					ArrayList<String> tit = dataFase1(xmlCorrectos);
					for (int i = 0; i < tit.size(); i++) {
						if(i==0){
						out.println("<input type='radio' name='titulacion' value='" + tit.get(i) + "' checked='true'>"+(i+1)+".- " + tit.get(i)
								+ "<br>");}
						else{out.println("<input type='radio' name='titulacion' value='" + tit.get(i) + "'>"+(i+1)+".- " + tit.get(i)
						+ "<br>");}
					}
					if(tit.size()>0){
					out.println("<br><input class='button' type='submit' value='Enviar'>");
					out.println("<input type='hidden' name='fase' value='2'>");
					}
					out.println("</form>");
					out.println("<form name='formulario2'>");
					out.println("<input id='but2' class='button' type='submit' value='Atras'>");
					out.println("<input type='hidden' name='fase' value='0'>");
					out.println("</form>");
					out.println("<span>Javier García Rodríguez</span>");
					out.println("</body>");
					out.println("</html>");
					break;
				}
			case 2:
				if (auto.equals("si")) {
					res.setCharacterEncoding("iso-8859-15");
					out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
					out.println("<materias>");
					try {
						ArrayList<String> l = dataFase2(xmlCorrectos, titulacion);
						for (int i = 0; i < l.size(); i = i + 2) {
							out.println("<materia curso='" + l.get(i + 1) + "'>" + l.get(i) + "</materia>");
						}
					} catch (Exception e) {
					}
					out.println("</materias>");
					break;
				} else {
					int n=1;
					out.println("<html>");
					out.println("<head>");
					out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
					out.println("<title>Fase_2</title>");
					out.println("</head>");
					out.println("<body>");
					out.println("<form name='formulario'>");
					out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
					out.println("<h2>Titulación=" + titulacion + " </h2><br>");
					out.println("<h3>Selecciona una materia:</h3><br>");
					try {
						ArrayList<String> l = dataFase2(xmlCorrectos, titulacion);
						for (int i = 0; i < l.size(); i = i + 2) {
							if(i==0){
							out.println("<input type='radio' name='materia' value='" + l.get(i) + "' checked='true'>"+n+".- " + l.get(i) + " ("
									+ l.get(i + 1) + "º)<br>");
							n++;}
							else{out.println("<input type='radio' name='materia' value='" + l.get(i) + "'>"+n+".- " + l.get(i) + " ("
									+ l.get(i + 1) + "º)<br>");
							n++;}
						}
					
					if(l.size()>0){
					out.println("<input type='hidden' name='titulacion' value='" + titulacion + "'>");
					out.println("<br><input class='button' type='submit' value='Enviar'>");
					out.println("<input type='hidden' name='fase' value='3'>");
					}
					} catch (Exception e) {
					}
					out.println("</form>");
					out.println("<form name='formulario2'>");
					out.println("<input id='but2' class='button' type='submit' value='Atras'>");
					out.println("<input type='hidden' name='fase' value='1'>");
					out.println("</form>");
					out.println("<form name='formulario3'>");
					out.println("<input id='but3' class='button' type='submit' value='Inicio'>");
					out.println("<input type='hidden' name='fase' value='0'>");
					out.println("</form>");
					out.println("<span>Javier García Rodríguez</span>");
					out.println("</body>");
					out.println("</html>");
					break;
				}
			case 3:
				if (auto.equals("si")) {
					res.setCharacterEncoding("iso-8859-15");
					out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
					out.println("<alumnos>");
					try {
						ArrayList<String> l = dataFase3(xmlCorrectos, titulacion, materia);
						for (int i = 0; i < l.size(); i = i + 2) {
							out.println("<alumno direccion='" + l.get(i + 1) + "'>" + l.get(i) + "</alumno>");
						}
					} catch (Exception e) {
					}
					out.println("</alumnos>");
					break;
				} else {
					int n=1;
					out.println("<html>");
					out.println("<head>");
					out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
					out.println("<title>Fase_3</title>");
					out.println("</head>");
					out.println("<body>");
					out.println("<form name='formulario'>");
					out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
					out.println("<h2>Titulación=" + titulacion + " ,Materia=" + materia + " </h2><br>");
					out.println("<h3>Selecciona un alumno:</h3><br>");
					try {
						ArrayList<String> l = dataFase3(xmlCorrectos, titulacion, materia);
						for (int i = 0; i < l.size(); i = i + 2) {
							if(i==0){
							out.println("<input type='radio' name='alumno' value='" + l.get(i) + "' checked='true'>"+n+".- "+ l.get(i) + " ("
									+ l.get(i + 1) + ")<br>");
							n++;}
							else{out.println("<input type='radio' name='alumno' value='" + l.get(i) + "'>"+n+".- " + l.get(i) + " ("
									+ l.get(i + 1) + ")<br>");
							n++;}
						}
					
						if(l.size()>0){
					out.println("<input type='hidden' name='titulacion' value='" + titulacion + "'>");
					out.println("<input type='hidden' name='materia' value='" + materia + "'>");
					out.println("<br><input class='button' type='submit' value='Enviar'>");
					out.println("<input type='hidden' name='fase' value='4'>");
						}
					} catch (Exception e) {
					}
					out.println("</form>");
					out.println("<form name='formulario2'>");
					out.println("<input id='but2' class='button' type='submit' value='Atras'>");
					out.println("<input type='hidden' name='fase' value='2'>");
					out.println("<input type='hidden' name='titulacion' value='" + titulacion + "'>");
					out.println("</form>");
					out.println("<form name='formulario3'>");
					out.println("<input id='but3' class='button' type='submit' value='Inicio'>");
					out.println("<input type='hidden' name='fase' value='0'>");
					out.println("</form>");
					out.println("<span>Javier García Rodríguez</span>");
					out.println("</body>");
					out.println("</html>");
					break;
				}
			case 4:
				if (auto.equals("si")) {
					res.setCharacterEncoding("iso-8859-15");
					out.println("<?xml version='1.0' encoding='iso-8859-15'?>");
					out.println("<convocatorias>");
					try {
						ArrayList<String> l = dataFase4(xmlCorrectos, titulacion, materia, alumno);
						for (int i = 0; i < l.size(); i = i + 2) {
							out.println("<convocatoria>");
							out.println("<nombre>" + l.get(i) + "</nombre>");
							out.println("<nota>" + l.get(i + 1) + "</nota>");
							out.println("</convocatoria>");
						}
					} catch (Exception e) {
					}
					out.println("</convocatorias>");

					break;
				} else {
					int n=1;
					out.println("<html>");
					out.println("<head>");
					out.println("<link rel='stylesheet' type='text/css' href="+css_loc+">");
					out.println("<title>Fase_4</title>");
					out.println("</head>");
					out.println("<body>");
					out.println("<form name='formulario2'>");
					out.println("<div><h1>Servicio de consulta de expedientes académicos (sint83)</h1></div><br>");
					out.println("<h2>Titulación=" + titulacion + " ,Materia=" + materia + " ,Alumno=" + alumno
							+ " </h2><br>");
					out.println("<h3>Estas son sus notas:</h3><br>");
					try {
						ArrayList<String> l = dataFase4(xmlCorrectos, titulacion, materia, alumno);
						for (int i = 0; i < l.size(); i = i + 2) {
							out.println(n+".- <b>Convocatoria</b>=" + l.get(i) + ", <b>Nota</b>=" + l.get(i + 1) + "<br>");
							n++;
						}
					} catch (Exception e) {
					}
					out.println("<br>");
					out.println("<input class='button' type='submit' value='Atras'><br>");
					out.println("<input type='hidden' name='fase' value='3'>");
					out.println("<input type='hidden' name='titulacion' value='" + titulacion + "'>");
					out.println("<input type='hidden' name='materia' value='" + materia + "'>");
					out.println("</form>");
					out.println("<form name='formulario3'>");
					out.println("<input id='but2' class='button' type='submit' value='Inicio'>");
					out.println("<input type='hidden' name='fase' value='0'>");
					out.println("</form>");
					out.println("<span>Javier García Rodríguez</span>");
					out.println("</body>");
					out.println("</html>");
					break;
				}
			}
		}
	}

	public ArrayList<String> dataFase1(HashMap<String, Element> map) {

		ArrayList<String> tit = new ArrayList<String>();
		for (String key : map.keySet()) {
			tit.add(key);
			Collections.sort(tit);
			// System.out.println(key);
		}

		return tit;

	}

	public ArrayList<String> dataFase2(HashMap<String, Element> map, String titulacion)
			throws XPathExpressionException {

		ArrayList<String> lista = new ArrayList<String>();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		Element root = map.get(titulacion);
		for (int j = 1; j < 5; j++) {
			NodeList nl = (NodeList) xpath.evaluate(
					"/Titulacion[NombreTit='" + titulacion + "']/Curso[@numero=" + j + "]/Materia/NombreMat", root,
					XPathConstants.NODESET);
			for (int i = 0; i < nl.getLength(); i++) {
				ArrayList<String> l = new ArrayList<String>();
				l.add(nl.item(i).getTextContent());
				Collections.sort(l);
				for (int k = 0; k < l.size(); k++) {
					lista.add(l.get(k));
					lista.add(Integer.toString(j));
				}
			}
		}
		return lista;

	}

	public ArrayList<String> dataFase3(HashMap<String,Element> map, String titulacion,String materia) throws XPathExpressionException{
		ArrayList<String> l= new ArrayList<String>();
		ArrayList<String> listadni= new ArrayList<String>();
		ArrayList<String> listadir_dni= new ArrayList<String>();
		ArrayList<String> listares= new ArrayList<String>();
		ArrayList<String> listares_dir= new ArrayList<String>();
		XPathFactory xpathFactory=XPathFactory.newInstance();
		XPath xpath=xpathFactory.newXPath();
		Element root= map.get(titulacion);
		NodeList dni= (NodeList)xpath.evaluate("/Titulacion[NombreTit='"+titulacion+"']/Curso/Materia[NombreMat='"+materia+"']/Convocatoria/Alumno/Dni",root,XPathConstants.NODESET);
		NodeList res= (NodeList)xpath.evaluate("/Titulacion[NombreTit='"+titulacion+"']/Curso/Materia[NombreMat='"+materia+"']/Convocatoria/Alumno/Residente", root, XPathConstants.NODESET);
	
		for (int i=0;i<dni.getLength();i++){
			listadni.add(dni.item(i).getTextContent());
			}
		
		Set<String> hashset = new LinkedHashSet<String>(listadni);
		hashset.addAll(listadni);
		listadni.clear();
		listadni.addAll(hashset);
		Collections.sort(listadni);
		
		for (int i=0;i<res.getLength();i++){
			listares.add(res.item(i).getTextContent());
			}
		
		Set<String> hashset2 = new LinkedHashSet<String>(listares);
		hashset2.addAll(listares);
		listares.clear();
		listares.addAll(hashset2);
		Collections.sort(listares);
	
		
		
		for(int i=0;i<listadni.size();i++){
		NodeList dni2= (NodeList)xpath.evaluate("/Titulacion[NombreTit='"+titulacion+"']/Curso/Materia[NombreMat='"+materia+"']/Convocatoria/Alumno[Dni='"+listadni.get(i)+"']/text()",root,XPathConstants.NODESET);
		for (int j=0;j<dni2.getLength();j++)	{
			if(dni2.item(j).getTextContent().trim().length()>1){
				listadir_dni.add(dni2.item(j).getTextContent().trim());

			}

		}}
		Set<String> hashset3 = new LinkedHashSet<String>(listadir_dni);
		hashset3.addAll(listadir_dni);
		listadir_dni.clear();
		listadir_dni.addAll(hashset3);
	
	
		for(int i=0;i<listares.size();i++){
			NodeList res2= (NodeList)xpath.evaluate("/Titulacion[NombreTit='"+titulacion+"']/Curso/Materia[NombreMat='"+materia+"']/Convocatoria/Alumno[Residente='"+listares.get(i)+"']/text()",root,XPathConstants.NODESET);
			for (int j=0;j<res2.getLength();j++)	{
				if(res2.item(j).getTextContent().trim().length()>1){
					listares_dir.add(res2.item(j).getTextContent().trim());
				}
			}
		}
		
		Set<String> hashset4 = new LinkedHashSet<String>(listares_dir);
		hashset4.addAll(listares_dir);
		listares_dir.clear();
		listares_dir.addAll(hashset4);
	
		for (int i=0;i<listadni.size();i++){
			l.add(listadni.get(i));
			l.add(listadir_dni.get(i));
		}
		
		for (int i=0;i<listares.size();i++){
			l.add(listares.get(i));
			l.add(listares_dir.get(i));
		}
	
	
	
	return l;
	}

	public static ArrayList<String> dataFase4(HashMap<String, Element> map, String titulacion, String materia,
			String alu) throws XPathExpressionException {
		ArrayList<String> l = new ArrayList<String>();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		Element root = map.get(titulacion);
		NodeList nl = (NodeList) xpath.evaluate(
				"/Titulacion[NombreTit='" + titulacion + "']/Curso/Materia[NombreMat='" + materia
						+ "']/Convocatoria/Alumno[Dni='" + alu + "']/Nota |" + "/Titulacion[NombreTit='" + titulacion
						+ "']/Curso/Materia[NombreMat='" + materia + "']/Convocatoria/Alumno[Residente='" + alu
						+ "']/Nota | " + "/Titulacion[NombreTit='" + titulacion + "']/Curso/Materia[NombreMat='"
						+ materia + "']/Convocatoria/Alumno[Dni='" + alu + "']/ancestor::Convocatoria/NombreConv | "
						+ "/Titulacion[NombreTit='" + titulacion + "']/Curso/Materia[NombreMat='" + materia
						+ "']/Convocatoria/Alumno[Residente='" + alu + "']/ancestor::Convocatoria/NombreConv",
				root, XPathConstants.NODESET);

		for (int i = 0; i < nl.getLength(); i++) {
			l.add(nl.item(i).getTextContent());
		}

		return l;

	}

}

