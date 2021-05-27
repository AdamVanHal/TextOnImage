
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

public class AppWindow {

	JCheckBox chckbxDateLocation;

	private JFrame frame;

	JComboBox<String> bordeColor;

	JComboBox<String> posicionTexto;

	JComboBox<String> logoPosition;

	JButton btnSave;

	DefaultListCellRenderer listRenderer;

	JLabel lblImage;

	private JTextField texto;

	final JFileChooser fc = new JFileChooser();

	LinkedList<File> fileList = null;

	File file = null;

	BufferedImage image = null;

	BufferedImage original = null;

	BufferedImage logo = null;

	TiffOutputSet outputSet = null;

	String coords = null;

	String date = null;

	double drawLocation = 0;

	int direction = 0;

	private JTextField compresion;

	private JTextField fontSize;

	JComboBox<String> colorTexto;

	void recalcular(boolean pintar) {

		BufferedImage imagen;

		if (chckbxDateLocation.isSelected()) {

			imagen = image;

			drawLocation = drawText(image, date, drawLocation);

			drawLocation = drawText(image, coords, drawLocation);
		}

		else {

			imagen = original;

			if (!texto.getText().isEmpty()) {

				drawLocation = drawText(original, texto.getText(), drawLocation);

			}

		}

		double ratioLabel = (double) lblImage.getHeight() / lblImage.getWidth();

		double ratioImage;

		ratioImage = (double) imagen.getHeight() / imagen.getWidth();

		if (pintar) {

			if (ratioLabel > ratioImage) {

				lblImage.setIcon(new ImageIcon(imagen.getScaledInstance(lblImage.getWidth(), -1, 0)));

			}

			else {

				lblImage.setIcon(new ImageIcon(imagen.getScaledInstance(-1, lblImage.getHeight(), 0)));

			}
		}
	}

	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {

			public void run() {

				try {

					AppWindow window = new AppWindow();

					window.frame.setVisible(true);
				}

				catch (Exception e) {
//
				}

			}

		});

	}

	public AppWindow() {

		listRenderer = new DefaultListCellRenderer();

		listRenderer.setHorizontalAlignment(DefaultListCellRenderer.CENTER);

		fc.setFileFilter(new FileFilter() {

			public boolean accept(File file) {

				boolean resultado = false;

				if (file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg")
						|| file.isDirectory()) {

					resultado = true;

				}

				return resultado;

			}

			public String getDescription() {

				return "JPG and JPEG images";

			}

		});

		fc.setMultiSelectionEnabled(true);

		fc.setAcceptAllFileFilterUsed(false);

		initialize();

	}

	private void abrirImagen(boolean pintar) {

		try {

			image = ImageIO.read(file);

			original = ImageIO.read(file);

			coords = null;

			date = null;

			drawLocation = 0;

			getExif(file);

			image = imageRotate(image, direction);

			original = imageRotate(original, direction);

			drawLocation = image.getHeight() * 0.97 - (3 * 1.1 * 70);

			recalcular(pintar);

			lblImage.getHeight();

			lblImage.getWidth();

			btnSave.setEnabled(true);

		}

		catch (Exception ex) {

		}

	}

	private void initialize() {

		frame = new JFrame();
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(AppWindow.class.getResource("/images/watermark.png")));

		frame.setBounds(50, 50, 850, 700);

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		frame.setResizable(false);

		frame.setLocationRelativeTo(null);

		JButton btnOpen = new JButton("Open File(s)");

		JButton btnLogoSel = new JButton("Choose Logo");

		btnLogoSel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				fc.setApproveButtonText("Open");

				fc.setDialogTitle("Get Logo");

				fc.setMultiSelectionEnabled(false);

				FileFilter oldFilter = fc.getFileFilter();

				FileFilter anyImage = new FileFilter() {

					public boolean accept(File file) {

						boolean resultado = false;

						if ((file.getName().toLowerCase().endsWith(".jpg")
								|| file.getName().toLowerCase().endsWith(".jpeg") || file.isDirectory())

								|| (file.getName().toLowerCase().endsWith(".png"))

								|| (file.getName().toLowerCase().endsWith(".bmp")
										|| file.getName().toLowerCase().endsWith(".wbmp"))

								|| (file.getName().toLowerCase().endsWith(".gif"))

						) {
							resultado = true;
						}

						return resultado;

					}

					public String getDescription() {

						return "Any Image";

					}

				};

				fc.setFileFilter(anyImage);

				int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {

					file = fc.getSelectedFile();

					try {

						logo = ImageIO.read(file);

					}

					catch (IOException e1) {

						JOptionPane.showMessageDialog(null, "Failed to Open Image");

					}

				}

				fc.setFileFilter(oldFilter);

				fc.removeChoosableFileFilter(anyImage);

			}

		});

		btnLogoSel.setEnabled(false);

		btnLogoSel.setBounds(341, 7, 142, 23);

		frame.getContentPane().add(btnLogoSel);

		JCheckBox chckbxAddLogo = new JCheckBox("Logo");

		chckbxAddLogo.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {

				if (chckbxAddLogo.isSelected()) {
					btnLogoSel.setEnabled(true);
				}

				else {
					btnLogoSel.setEnabled(false);
				}

			}

		});

		chckbxAddLogo.setBounds(256, 7, 79, 23);

		frame.getContentPane().add(chckbxAddLogo);

		chckbxDateLocation = new JCheckBox("Add GPS Info");

		chckbxDateLocation.setSelected(true);

		chckbxDateLocation.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {

				if (lblImage.getIcon() != null) {
					recalcular(false);
				}

			}

		});

		chckbxDateLocation.setBounds(489, 7, 127, 23);

		frame.getContentPane().add(chckbxDateLocation);

		btnSave = new JButton("");
		btnSave.setIcon(new ImageIcon(AppWindow.class.getResource("/images/save.png")));

		texto = new JTextField();

		texto.setFont(new Font("Dialog", Font.PLAIN, 16));

		texto.setToolTipText("Add a short note to the bottom of the image");

		texto.setBounds(636, 563, 180, 33);

		frame.getContentPane().add(texto);

		texto.setColumns(10);

		texto.getText();

		lblImage = new JLabel("");
		lblImage.setHorizontalAlignment(SwingConstants.CENTER);

		lblImage.setBounds(7, 41, 627, 619);

		frame.getContentPane().add(lblImage);

		btnOpen.setBounds(7, 7, 243, 23);

		btnOpen.addMouseListener(new MouseAdapter() {

			@Override

			public void mouseReleased(MouseEvent e) {

				fc.setDialogTitle("Open");

				fc.setApproveButtonText("Open");

				fc.setMultiSelectionEnabled(true);

				int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {

					fileList = new LinkedList<File>(Arrays.asList(fc.getSelectedFiles()));

					file = fileList.pop();

					abrirImagen(true);

				}

			}

		});

		frame.getContentPane().setLayout(null);

		frame.getContentPane().add(btnOpen);

		btnSave.setBounds(704, 608, 49, 52);

		btnSave.setEnabled(false);

		btnSave.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				if (!texto.getText().isEmpty()) {

					drawLocation = drawText(image, texto.getText(), drawLocation);

				}

				fc.setApproveButtonText("Save");

				fc.setDialogTitle("Save");

				fc.setMultiSelectionEnabled(false);

				fc.setSelectedFile(file);

				int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {

					file = fc.getSelectedFile();

					if (chckbxAddLogo.isSelected() && logo != null) {

						Graphics2D graphic = image.createGraphics();

						switch (logoPosition.getSelectedIndex()) {

						case 0:

							graphic.drawImage(logo, (image.getWidth() - logo.getWidth()) - logo.getWidth(), 0, null);

							break;

						case 1:

							graphic.drawImage(logo, (image.getWidth() - logo.getWidth()), 0, null);
							break;

						case 2:

							graphic.drawImage(logo, (image.getWidth() - logo.getWidth()) - logo.getWidth(),
									(image.getHeight() - logo.getHeight()), null);
							break;

						case 3:

							graphic.drawImage(logo, (image.getWidth() - logo.getWidth()),
									(image.getHeight() - logo.getHeight()), null);
							break;

						case 4:

							graphic.drawImage(logo, image.getWidth() / 2 - logo.getWidth() / 2,
									image.getHeight() / 2 - logo.getHeight() / 2, null);
							break;

						}

						graphic.dispose();

					}

					final ImageWriter imWrite = ImageIO.getImageWritersByFormatName("jpg").next();

					JPEGImageWriteParam jpgParams = new JPEGImageWriteParam(null);

					jpgParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

					float compress = 0.95f;

					try {

						compress = Float.parseFloat(compresion.getText()) / 100;

					}

					catch (Exception e1) {

						compress = 0.95f;

					}

					jpgParams.setCompressionQuality(compress);

					FileImageOutputStream fileOutStream = null;

					try {

						fileOutStream = new FileImageOutputStream(file);
					}

					catch (IOException e4) {

						JOptionPane.showMessageDialog(null, "Failed to Create File");

					}

					imWrite.setOutput(fileOutStream);

					try {

						imWrite.write(null, new IIOImage(image, null, null), jpgParams);

						fileOutStream.close();

					}

					catch (IOException e2) {

						JOptionPane.showMessageDialog(null, "Failed to Save Image");

					}

					writeExif(file, outputSet);

					if (!fileList.isEmpty()) {

						file = fileList.pop();

						try {

							image = ImageIO.read(file);

							original = ImageIO.read(file);

							coords = null;

							date = null;

							drawLocation = 0;

							getExif(file);

							image = imageRotate(image, direction);

							original = imageRotate(original, direction);

							drawLocation = image.getHeight() * 0.97 - (3 * 1.1 * 70);

							if (chckbxDateLocation.isSelected()) {

								drawLocation = drawText(image, date, drawLocation);

								drawLocation = drawText(image, coords, drawLocation);

							}

							else {
								drawLocation = drawText(original, texto.getText(), drawLocation);
							}

							recalcular(false);

						}

						catch (Exception ex) {

							ex.printStackTrace();
						}
					}

					else {

						lblImage.setIcon(null);

						btnSave.setEnabled(false);

					}
				}

			}

		});

		frame.getContentPane().add(btnSave);

		compresion = new JTextField();

		compresion.setFont(new Font("Dialog", Font.PLAIN, 16));

		compresion.setText("95");

		compresion.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					Integer.parseInt(compresion.getText());

				}

				catch (Exception e1) {

					compresion.setText("95");

				}

			}

		});

		compresion.setHorizontalAlignment(SwingConstants.CENTER);

		compresion.setBounds(775, 7, 41, 33);

		frame.getContentPane().add(compresion);

		compresion.setColumns(10);

		JLabel lblNewLabel = new JLabel("% Compresion");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 16));

		lblNewLabel.setBounds(630, 6, 127, 23);

		frame.getContentPane().add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("");

		lblNewLabel_1.setIcon(new ImageIcon(AppWindow.class.getResource("/images/size_Text.png")));

		lblNewLabel_1.setBounds(646, 51, 56, 48);

		frame.getContentPane().add(lblNewLabel_1);

		fontSize = new JTextField();

		fontSize.setHorizontalAlignment(SwingConstants.CENTER);

		fontSize.setFont(new Font("Dialog", Font.PLAIN, 16));

		fontSize.setText("70");

		fontSize.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				int size = 70;

				try {

					size = Integer.parseInt(fontSize.getText());

					if (size > 125) {
						size = 125;
					}

				}

				catch (Exception e1) {

				}

				fontSize.setText("" + size);

			}
		});

		fontSize.setBounds(707, 64, 109, 33);

		frame.getContentPane().add(fontSize);

		fontSize.setColumns(10);

		colorTexto = new JComboBox<String>();

		colorTexto.setFont(new Font("Dialog", Font.PLAIN, 16));

		colorTexto.setBounds(636, 381, 180, 33);

		colorTexto.addItem("WHITE");
		colorTexto.addItem("BLUE");
		colorTexto.addItem("CYAN");
		colorTexto.addItem("DARK GRAY");
		colorTexto.addItem("LIGHT GRAY");
		colorTexto.addItem("GREEN");
		colorTexto.addItem("MAGENTA");
		colorTexto.addItem("ORANGE");
		colorTexto.addItem("PINK");
		colorTexto.addItem("RED");
		colorTexto.addItem("BLACK");
		colorTexto.addItem("YELLOW");

		frame.getContentPane().add(colorTexto);

		colorTexto.setRenderer(listRenderer);

		posicionTexto = new JComboBox<String>();

		posicionTexto.setRenderer(listRenderer);

		posicionTexto.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {

				try {

					abrirImagen(true);

				}

				catch (Exception e) {
				}

			}

		});

		posicionTexto.setFont(new Font("Dialog", Font.PLAIN, 16));

		posicionTexto.setBounds(636, 461, 180, 30);

		posicionTexto.addItem("Arriba izquierda");

		posicionTexto.addItem("Arriba derecha");

		posicionTexto.addItem("Abajo izquierda");

		posicionTexto.addItem("Abajo derecha");

		posicionTexto.addItem("Centro");

		frame.getContentPane().add(posicionTexto);

		logoPosition = new JComboBox<String>();

		logoPosition.setRenderer(listRenderer);

		logoPosition.setFont(new Font("Dialog", Font.PLAIN, 16));

		logoPosition.setBounds(636, 160, 180, 34);

		logoPosition.addItem("Arriba izquierda");

		logoPosition.addItem("Arriba derecha");

		logoPosition.addItem("Abajo izquierda");

		logoPosition.addItem("Abajo derecha");

		logoPosition.addItem("Centro");

		frame.getContentPane().add(logoPosition);

		bordeColor = new JComboBox<String>();

		bordeColor.setFont(new Font("Dialog", Font.PLAIN, 16));

		bordeColor.setBounds(636, 276, 180, 33);

		bordeColor.addItem("BLACK");
		bordeColor.addItem("BLUE");
		bordeColor.addItem("CYAN");
		bordeColor.addItem("DARK GRAY");
		bordeColor.addItem("LIGHT GRAY");
		bordeColor.addItem("GREEN");
		bordeColor.addItem("MAGENTA");
		bordeColor.addItem("ORANGE");
		bordeColor.addItem("PINK");
		bordeColor.addItem("RED");
		bordeColor.addItem("WHITE");
		bordeColor.addItem("YELLOW");

		frame.getContentPane().add(bordeColor);

		bordeColor.setRenderer(listRenderer);

		JLabel colortext = new JLabel("Text Color");
		colortext.setIcon(new ImageIcon(AppWindow.class.getResource("/images/color.png")));

		colortext.setHorizontalAlignment(SwingConstants.CENTER);

		colortext.setFont(new Font("Dialog", Font.PLAIN, 16));

		colortext.setBounds(638, 324, 180, 52);

		frame.getContentPane().add(colortext);

		JLabel lblPosicionTexto = new JLabel("Posicion texto");

		lblPosicionTexto.setFont(new Font("Dialog", Font.PLAIN, 16));

		lblPosicionTexto.setBounds(664, 426, 120, 23);

		frame.getContentPane().add(lblPosicionTexto);

		JLabel lblBorderColor = new JLabel("Border Color");
		lblBorderColor.setIcon(new ImageIcon(AppWindow.class.getResource("/images/color.png")));

		lblBorderColor.setHorizontalAlignment(SwingConstants.CENTER);

		lblBorderColor.setFont(new Font("Dialog", Font.PLAIN, 16));

		lblBorderColor.setBounds(636, 216, 180, 48);

		frame.getContentPane().add(lblBorderColor);

		JLabel lblNewLabel_2 = new JLabel("Texto");
		lblNewLabel_2.setIcon(new ImageIcon(AppWindow.class.getResource("/images/write.png")));

		lblNewLabel_2.setFont(new Font("Dialog", Font.PLAIN, 16));

		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);

		lblNewLabel_2.setBounds(636, 503, 180, 48);

		frame.getContentPane().add(lblNewLabel_2);

		JLabel lblPositionOfLogo = new JLabel("Logo Position");

		lblPositionOfLogo.setHorizontalAlignment(SwingConstants.CENTER);

		lblPositionOfLogo.setFont(new Font("Dialog", Font.PLAIN, 16));

		lblPositionOfLogo.setBounds(636, 125, 180, 23);

		frame.getContentPane().add(lblPositionOfLogo);

	}

	private void getExif(File imageFile) {

		outputSet = null;

		byte imageBytes[] = null;

		ImageMetadata metadata = null;

		TiffImageMetadata.GPSInfo gpsInfo = null;

		try {

			imageBytes = Files.readAllBytes(imageFile.toPath());

			metadata = Imaging.getMetadata(imageBytes);

			if (metadata != null && imageBytes != null) {

				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

				final TiffImageMetadata exif = jpegMetadata.getExif();

				direction = 0;

				TiffField orientation = exif.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);

				if (orientation != null) {
					direction = orientation.getIntValue();
				}

				gpsInfo = exif.getGPS();

				coords = "";

				date = "";

				DecimalFormat df = new DecimalFormat("0.00000");

				coords = df.format(Math.abs(gpsInfo.getLatitudeAsDegreesNorth())) + "� " + gpsInfo.latitudeRef + ", "
						+ df.format(Math.abs(gpsInfo.getLongitudeAsDegreesEast())) + "� " + gpsInfo.longitudeRef;

				LocalDate fecha = LocalDate.now();

				date = fecha.getDayOfMonth() + "/" + fecha.getMonthValue() + "/" + fecha.getYear();

				outputSet = exif.getOutputSet();

				if (outputSet != null && direction > 1) {

					outputSet.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);

				}

			}

		}

		catch (Exception ex) {
//
		}

	}

	private double drawText(BufferedImage bImage, String text, double location) {

		if (text == null) {

			return location;

		}

		Graphics2D graphic = bImage.createGraphics();

		int size = 70;

		try {

			size = Integer.parseInt(fontSize.getText());
		}

		catch (Exception e) {

			size = 70;

		}
		try {

			Font overlayFont = new Font(Font.SANS_SERIF, Font.BOLD, size);

			TextLayout textLayout = new TextLayout(text, overlayFont, new FontRenderContext(null, false, false));

			graphic.setFont(overlayFont);

			graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphic.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			AffineTransform transform = new AffineTransform();

			switch (posicionTexto.getSelectedIndex()) {

			case 0:

				location /= 3;

				transform.translate(bImage.getWidth() * 0.03, location);

				break;

			case 1:

				location /= 3;

				transform.translate(bImage.getWidth() / 1.5, location);

				break;

			case 2:

				location += 20;

				transform.translate(bImage.getWidth() * 0.03, location);

				break;

			case 3:

				transform.translate(bImage.getWidth() - bImage.getWidth() / 3, location);

				break;

			case 4:

				location /= 1.7;

				location += 20;

				transform.translate(bImage.getWidth() / 3, location);

				break;

			}

			location = location + textLayout.getAscent() * 1.1;

			Shape outline = textLayout.getOutline(transform);

			Color color, textoColor;

			switch (bordeColor.getSelectedIndex()) {

			case 0:

				color = Color.BLACK;

				break;

			case 1:

				color = Color.BLUE;

				break;

			case 2:

				color = Color.CYAN;

				break;

			case 3:

				color = Color.DARK_GRAY;

				break;
			case 4:

				color = Color.LIGHT_GRAY;

				break;

			case 5:

				color = Color.GREEN;

				break;

			case 6:

				color = Color.MAGENTA;

				break;

			case 7:

				color = Color.ORANGE;

				break;

			case 8:

				color = Color.PINK;

				break;

			case 9:

				color = Color.RED;

				break;

			case 10:

				color = Color.WHITE;

				break;

			case 11:

				color = Color.YELLOW;

				break;

			default:

				color = Color.BLACK;

				break;

			}

			switch (colorTexto.getSelectedIndex()) {

			case 0:

				textoColor = Color.WHITE;

				break;

			case 1:

				textoColor = Color.BLUE;

				break;

			case 2:

				textoColor = Color.CYAN;

				break;

			case 3:

				textoColor = Color.DARK_GRAY;

				break;
			case 4:

				textoColor = Color.LIGHT_GRAY;

				break;

			case 5:

				textoColor = Color.GREEN;

				break;

			case 6:

				textoColor = Color.MAGENTA;

				break;

			case 7:

				textoColor = Color.ORANGE;

				break;

			case 8:

				textoColor = Color.PINK;

				break;

			case 9:

				textoColor = Color.RED;

				break;

			case 10:

				textoColor = Color.BLACK;

				break;

			case 11:

				textoColor = Color.YELLOW;

				break;

			default:

				textoColor = Color.WHITE;

				break;

			}

			graphic.setColor(color);

			graphic.setStroke(new BasicStroke(10));

			graphic.draw(outline);

			graphic.setColor(textoColor);

			graphic.fill(outline);

			graphic.dispose();
		}

		catch (Exception e) {
		}

		return location;

	}

	private void writeExif(File dest, TiffOutputSet tiffOutput) {

		if (tiffOutput != null) {

			try {

				new ExifRewriter().updateExifMetadataLossless(Files.readAllBytes(dest.toPath()),
						new FileOutputStream(file), tiffOutput);
			}

			catch (Exception ex) {
//
			}

		}

	}

	private BufferedImage imageRotate(BufferedImage image, int exifDirection) {

		AffineTransform transform = new AffineTransform();

		BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

		switch (exifDirection) {

		case 0:

			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

			break;

		case 1:

			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

			break;

		case 2:

			transform.translate(-image.getWidth(), 0);

			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

			break;

		case 3:

			transform.translate(image.getWidth(), image.getHeight());

			transform.rotate(Math.PI);

			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

			break;

		case 4:

			transform.scale(1.0, -1.0);

			transform.translate(0, -image.getHeight());

			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

			break;

		case 5:

			transform.rotate(-Math.PI / 2);

			transform.scale(-1.0, 1.0);

			temp = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

			break;

		case 6:

			transform.translate(image.getHeight(), 0);

			transform.rotate(Math.PI / 2);

			temp = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

			break;

		case 7:

			transform.scale(-1.0, 1.0);

			transform.translate(-image.getHeight(), 0);

			transform.translate(0, image.getWidth());

			transform.rotate(3 * Math.PI / 2);

			temp = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

			break;

		case 8:

			transform.translate(0, image.getWidth());

			transform.rotate(3 * Math.PI / 2);

			temp = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

			break;

		default:

			break;

		}

		Graphics2D g = temp.createGraphics();

		g.transform(transform);

		g.drawImage(image, null, 0, 0);

		g.dispose();

		return temp;

	}
}
