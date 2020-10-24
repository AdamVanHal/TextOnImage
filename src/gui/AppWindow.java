package gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import java.awt.BasicStroke;
import java.awt.Color;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;



public class AppWindow {

	private JFrame frame;
	JLabel lblImage;
	private JTextField textField;

	final JFileChooser fc = new JFileChooser(); //Create a file chooser for selecting our images
	LinkedList<File> fileList = null;
	File file = null; //location of our image file

	BufferedImage image = null; //Image that we will be manipulating
	TiffOutputSet outputSet = null; //stores EXIF data to write to new image

	String coords = null;
	String date = null;
	double drawLocation = 0;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AppWindow window = new AppWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AppWindow() {

		//set what types of files to display in chooser
		fc.setFileFilter(new FileFilter(){
			public boolean accept(File file) {
				if(file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg") || file.isDirectory()) {
					return true;}
				return false;
			}
			public String getDescription() {
				return "JPG and JPEG images";
			}
		});
		fc.setMultiSelectionEnabled(true);//allow multiselect
		fc.setAcceptAllFileFilterUsed(false);//disable the file filter that accepts anything

		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(50, 50, 850, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		

		JButton btnOpen = new JButton("Open File(s)");
		JButton btnSave = new JButton("Save File");;

		textField = new JTextField();
		textField.setBounds(7, 630, 820, 23);
		frame.getContentPane().add(textField);
		textField.setColumns(10);
		textField.getText();

		lblImage = new JLabel("");
		//lblImage.setIcon(new ImageIcon("C:\\Users\\Adam\\Documents\\IMG_20201003_184642306.jpg"));
		lblImage.setBounds(7, 41, 820, 585);
		frame.getContentPane().add(lblImage);

		btnOpen.setBounds(7, 7, 405, 23);
		btnOpen.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				//open file chooser
				fc.setDialogTitle("Open");
				fc.setApproveButtonText("Open");
				fc.setMultiSelectionEnabled(true);
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					fileList = new LinkedList<File>(Arrays.asList(fc.getSelectedFiles()));//list of all files chosen by user
					file = fileList.pop();

					try
					{
						image = ImageIO.read(file);
						//image = Imaging.getBufferedImage(file); //different library, not sure if different, use if there are problems
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed to Open Image");
						return;
						//failed to make image, handle exception here
						//System.exit(1);
					}

					getExif(file);


					drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
					drawLocation = drawText(image,date,drawLocation); //add date to image
					drawLocation = drawText(image,coords,drawLocation); //add coords to image

					//draw image to ui
					//lblImage.getGraphics().drawImage(image, 0, 0, lblImage.getHeight(), lblImage.getWidth(), 0, 0, image.getHeight(), image.getWidth(),null);
					//decide if the image aspect ratio is wider or taller than label to decide how to scale it
					double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
					double ratioImage = image.getHeight()/image.getWidth();
					if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
						lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
					}else {
						lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
					}
					
					btnSave.setEnabled(true);//enable the save button because we have an image now
				} else {
					//action on cancel, probably nothing
				}//end if ApproveOption
			}//end mouse event
		});
		frame.getContentPane().setLayout(null);
		frame.getContentPane().add(btnOpen);

		
		btnSave.setBounds(422, 7, 405, 23);
		btnSave.setEnabled(false);
		btnSave.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if(!btnSave.isEnabled()) {
					return;//button is disabled, return immediately and do nothing
				}
				//draw user text
				if(!textField.getText().isEmpty()) {
					drawLocation = drawText(image, textField.getText(), drawLocation);
				}
				//save image
				fc.setApproveButtonText("Save");
				fc.setDialogTitle("Save");
				fc.setMultiSelectionEnabled(false);//disable multiselect and set the location to the current file so saving makes sense and does not show more files
				fc.setSelectedFile(file);
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					//anything else we want to do with the file

					//create and image writer and set it to the quality settings we want
					final ImageWriter imWrite = ImageIO.getImageWritersByFormatName("jpg").next();
					JPEGImageWriteParam jpgParams = new JPEGImageWriteParam(null);
					jpgParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					jpgParams.setCompressionQuality(0.95f);//set quality to 95% using float

					//set location for writer by turning our file into an output stream and passing that to setOutput so we can close it later.
					FileImageOutputStream fileOutStream = null;
					try {
						fileOutStream = new FileImageOutputStream(file);
					} catch (IOException e4) {
						e4.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed to Create File");
						return;
					}
					imWrite.setOutput(fileOutStream);
					try {
						imWrite.write(null,new IIOImage(image,null,null),jpgParams);//write image using params set above. Still need to add EXIF data
						fileOutStream.close();//close the file to release locks
						//ImageIO.write(image, "jpg", file); //this method blows away existing EXIF data, find way to preserve. Write exif back to image after making?
						//Imaging.writeImage(image, file, ImageFormats.JPEG, null); //library does not support JPG writing so can not use this
					} catch (IOException e2) {
						e2.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed to Save Image");
						return;
					}//end try/catch

					//add EXIF data using Apache library
					writeExif(file, outputSet);
					
					//set up next image in multiselect if there is one
					if (!fileList.isEmpty()) {
						file = fileList.pop();
						try
						{
							image = ImageIO.read(file);
							//image = Imaging.getBufferedImage(file); //different library, not sure if different, use if there are problems
						}
						catch (Exception ex)
						{
							//failed to make image, handle exception here
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, "Failed to Open Image");
							return;
						}

						getExif(file);

						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						drawLocation = drawText(image,date,drawLocation); //add date to image
						drawLocation = drawText(image,coords,drawLocation); //add coords to image

						//draw image to ui
						//lblImage.getGraphics().drawImage(image, 0, 0, lblImage.getHeight(), lblImage.getWidth(), 0, 0, image.getHeight(), image.getWidth(),null);
						//decide if the image aspect ratio is wider or taller than label to decide how to scale it
						double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					} else { //no file is next
						lblImage.setIcon(null);
						btnSave.setEnabled(false);//disable the save button because there is no image to save
					}
				} else {
					//action on cancel, probably nothing
				}//end if Approve_Option
			}//end mouse event
		});
		frame.getContentPane().add(btnSave);



	}
	private void getExif(File imageFile) {
		byte imageBytes[]=null;
		//get the byte array of the original file of the image so that we can use it directly to find the EXIF data. Assumes the image is a jpeg
		try {
			imageBytes = Files.readAllBytes(imageFile.toPath());
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Open File");
			return;
		}
		//use Apache library to calculate the metadata and convert it to a TIFF style EXIF 
		ImageMetadata metadata = null;
		try {
			metadata = Imaging.getMetadata(imageBytes);
		} catch (ImageReadException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Image Format Error");
			return;
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error I/O Failure");
			return;
		}
		final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		final TiffImageMetadata exif = jpegMetadata.getExif();

		//Extract GPS metadata from the image
		TiffImageMetadata.GPSInfo gpsInfo = null;
		try {
			gpsInfo = exif.getGPS();
		} catch (ImageReadException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Parse GPS Metadata");
		}
		coords = "";
		date = "";
		//Construct GPS Coord String
		DecimalFormat df = new DecimalFormat("0.00000");

		try {
			coords = df.format(Math.abs(gpsInfo.getLatitudeAsDegreesNorth())) + "° " + gpsInfo.latitudeRef + ", " + df.format(Math.abs(gpsInfo.getLongitudeAsDegreesEast())) + "° " + gpsInfo.longitudeRef;
		} catch (ImageReadException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Parse GPS Metadata");
		}
		//construct the date string
		DateFormat dateFormat = new SimpleDateFormat("''yyyy:MM:dd HH:mm:ss''", Locale.ENGLISH);
		DateFormat dateRenderFormat = new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.ENGLISH);
		Date imageDate = null;
		try {
			imageDate = dateFormat.parse(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
		} catch (ParseException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Parse Date Metadata");
		}
		date = dateRenderFormat.format(imageDate);

		try {
			outputSet = exif.getOutputSet();
		} catch (ImageWriteException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Copy Metadata, Cannot Save Metadata with New Image");
		}

		//print gps and date info to SYSO for debugging reasons, remove in final release. 
		//      System.out.println(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
		//      System.out.println(date);
		//      System.out.println(gpsInfo.latitudeDegrees + "° " + gpsInfo.latitudeMinutes + "\' " + gpsInfo.latitudeSeconds.doubleValue() + "\" " + gpsInfo.latitudeRef);
		//      System.out.println(gpsInfo.longitudeDegrees + "° " + gpsInfo.longitudeMinutes + "\' " + gpsInfo.longitudeSeconds.doubleValue() + "\" " + gpsInfo.longitudeRef);
		//      System.out.println(coords);


		//      TiffOutputDirectory gpsCoords=null;
		//      gpsCoords = outputSet.getGPSDirectory();
		//      
		//      System.out.println(gpsCoords.findField(2));
		//      System.out.println(gpsCoords.findField(4));
		//      TiffOutputField field = gpsCoords.findField(2);

	}

	//Takes a string and an vertical position. draws the string on the image at that position, returning the original position plus the height of the text
	private double drawText(BufferedImage bImage, String text, double location) {
		Graphics2D graphic = bImage.createGraphics();
		//create shape of the outer edge of the text. This will allow us to draw the outline in one color and then fill with solid color
		Font overlayFont = new Font(Font.SANS_SERIF,Font.BOLD,70);
		TextLayout textLayout = new TextLayout(text,overlayFont,new FontRenderContext(null,false,false)); //turn string into style we want for rendering
		graphic.setFont(overlayFont);
		//suggest that it use antiAliaing since this is text
		graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		graphic.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		AffineTransform transform = new AffineTransform();

		transform.translate(bImage.getWidth()*0.03, location);//set location of outline for rendering in the bottom of the image
		location = location + textLayout.getAscent()*1.1;//drop down a line
		Shape outline = textLayout.getOutline(transform); //get the outer edges of the text
		//Draw a string on the image
		//gImage.drawString(text, 100, 100);//string does not quite line up if you use this, using fill instead.
		graphic.setColor(Color.BLACK);
		graphic.setStroke(new BasicStroke(10));
		graphic.draw(outline);//adds the outline
		graphic.setColor(Color.getHSBColor(28/360f, 1f, 1f));
		graphic.fill(outline); //fill in the outline with color
		return location;
	}

	//simple helper that takes a file and a set of EXIF data and rewrites the image file with the EXIF
	private void writeExif(File dest, TiffOutputSet tiffOutput ) {
		try {
			new ExifRewriter().updateExifMetadataLossless(Files.readAllBytes(dest.toPath()), new FileOutputStream(file), tiffOutput);
		} catch (ImageReadException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Image Format did not Match Expectation, Metadata Save Failed");
			return;
		} catch (ImageWriteException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Save Image with Metadata Copy");
			return;
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File Missing when Adding Metadata");
			return;
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File Failure when Adding Metadata");
			return;
		}
	}

}
