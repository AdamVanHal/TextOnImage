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
import java.awt.image.WritableRaster;
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
import org.apache.commons.imaging.formats.tiff.TiffField;
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
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;



public class AppWindow {

	private JFrame frame;
	JLabel lblImage;
	private JTextField textField;

	final JFileChooser fc = new JFileChooser(); //Create a file chooser for selecting our images
	LinkedList<File> fileList = null;
	File file = null; //location of our image file

	BufferedImage image = null; //Image that we will be manipulating
	BufferedImage original = null; //Image to store unedited version in case we want to remove changes
	BufferedImage logo = null; //Image to store image to be used for logo
	TiffOutputSet outputSet = null; //stores EXIF data to write to new image

	String coords = null;
	String date = null;
	double drawLocation = 0;
	int direction = 0;


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
		JCheckBox chckbxLocation = new JCheckBox("Add GPS Info");
		JCheckBox chckbxTimestamp = new JCheckBox("Add Timestamp");
		
		JButton btnLogoSel = new JButton("Choose Logo");
		btnLogoSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//get logo image
				fc.setApproveButtonText("Open");
				fc.setDialogTitle("Get Logo");
				fc.setMultiSelectionEnabled(false);//disable multiselect 
				//save original filter and add a new filter that allows more image types
				FileFilter oldFilter = fc.getFileFilter();
				FileFilter anyImage = new FileFilter(){
					public boolean accept(File file) {
						//jpeg
						if(file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg") || file.isDirectory()) {
							return true;
						}
						//png
						if(file.getName().toLowerCase().endsWith(".png")) {
							return true;
						}
						//bmp
						if(file.getName().toLowerCase().endsWith(".bmp") || file.getName().toLowerCase().endsWith(".wbmp")) {
							return true;
						}
						//gif
						if(file.getName().toLowerCase().endsWith(".gif")) {
							return true;
						}
						return false;
					}
					public String getDescription() {
						return "Any Image";
					}
				};
				fc.setFileFilter(anyImage);
				
				int returnVal = fc.showOpenDialog(frame);//open the dialog and retrieve the selected file for use
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					try {
						logo = ImageIO.read(file);
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed to Open Image");
						return;
					}
				}else {
					//action was probably canceled, do anything needed to clean up here.
				}
				//restore the old filter and remove the any image option
				fc.setFileFilter(oldFilter);
				fc.removeChoosableFileFilter(anyImage);
			}
		});
		btnLogoSel.setEnabled(false);
		btnLogoSel.setBounds(300, 7, 113, 23);
		frame.getContentPane().add(btnLogoSel);
		
		JCheckBox chckbxAddLogo = new JCheckBox("Add Logo");
		chckbxAddLogo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(chckbxAddLogo.isSelected()) {
					btnLogoSel.setEnabled(true);
				}else {
					btnLogoSel.setEnabled(false);
				}
			}
		});
		chckbxAddLogo.setBounds(215, 7, 79, 23);
		frame.getContentPane().add(chckbxAddLogo);
		
		
		chckbxLocation.setSelected(true);
		chckbxLocation.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(lblImage.getIcon() != null) {
					double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
					if(chckbxLocation.isSelected()) {//are we displaying the gps data on image?
						//reset the image and draw appropriate text on it
						image = bytecopy(original);
						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						if(chckbxTimestamp.isSelected()) {
							drawLocation = drawText(image,date,drawLocation); //add date to image
						}
						drawLocation = drawText(image,coords,drawLocation); //add coords to image
						//calculate image aspect ration so we can scale it properly
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}else {
						//reset the image and draw appropriate text on it
						image = bytecopy(original);
						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						if(chckbxTimestamp.isSelected()) {
							drawLocation = drawText(image,date,drawLocation); //add date to image
						}
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}
				}
			}
		});
		chckbxLocation.setBounds(419, 7, 99, 23);
		frame.getContentPane().add(chckbxLocation);
		
		
		chckbxTimestamp.setSelected(true);
		chckbxTimestamp.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(lblImage.getIcon() != null) {
					double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
					if(chckbxTimestamp.isSelected()) {//are we displaying the time stamp data on image?
						image = bytecopy(original);
						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						drawLocation = drawText(image,date,drawLocation); //add date to image
						if(chckbxLocation.isSelected()) {
							drawLocation = drawText(image,coords,drawLocation); //add coords to image
						}
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}else {
						image = bytecopy(original);
						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						if(chckbxLocation.isSelected()) {
							drawLocation = drawText(image,coords,drawLocation); //add coords to image
						}
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}
				}
			}
		});
		chckbxTimestamp.setBounds(520, 7, 113, 23);
		frame.getContentPane().add(chckbxTimestamp);
		
		JButton btnSave = new JButton("Save File");;

		textField = new JTextField();
		textField.setToolTipText("Add a short note to the bottom of the image");
		textField.setBounds(57, 630, 770, 23);
		frame.getContentPane().add(textField);
		textField.setColumns(10);
		textField.getText();

		lblImage = new JLabel("");
		lblImage.setBounds(7, 41, 820, 585);
		frame.getContentPane().add(lblImage);

		btnOpen.setBounds(7, 7, 197, 23);
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
						original = ImageIO.read(file);
						//image = Imaging.getBufferedImage(file); //Do not use, only limited support for jpg
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed to Open Image");
						return;
						//failed to make image, handle exception here
						//System.exit(1);
					}
					
					//new image, set all globals to blank
					coords = null;
					date = null;
					drawLocation = 0;

					getExif(file);
					
					//fix image rotation
					image = imageRotate(image,direction);
					original = imageRotate(original,direction);

					drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
					drawLocation = drawText(image,date,drawLocation); //add date to image
					drawLocation = drawText(image,coords,drawLocation); //add coords to image

					//draw image to ui
					//lblImage.getGraphics().drawImage(image, 0, 0, lblImage.getHeight(), lblImage.getWidth(), 0, 0, image.getHeight(), image.getWidth(),null);
					//decide if the image aspect ratio is wider or taller than label to decide how to scale it
					double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
					if(chckbxLocation.isSelected()) {//are we displaying the gps data on image?
						double ratioImage = image.getHeight()/image.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}else {
						double ratioImage = original.getHeight()/original.getWidth();
						if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
							lblImage.setIcon(new ImageIcon(original.getScaledInstance(lblImage.getWidth(), -1, 0)));
						}else {
							lblImage.setIcon(new ImageIcon(original.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
						}
					}
					
					btnSave.setEnabled(true);//enable the save button because we have an image now
				} else {
					//action on cancel, probably nothing
				}//end if ApproveOption
			}//end mouse event
		});
		frame.getContentPane().setLayout(null);
		frame.getContentPane().add(btnOpen);

		
		btnSave.setBounds(639, 7, 188, 23);
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
					
					//add logo if desired
					if(chckbxAddLogo.isSelected() && logo != null) {
						Graphics2D graphic = image.createGraphics();
						graphic.drawImage(logo, (image.getWidth()-logo.getWidth())-80, (image.getHeight()-logo.getHeight())-80, null);
						graphic.dispose();
					}
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
							original = ImageIO.read(file);//original to save and reuse if needed
							//image = Imaging.getBufferedImage(file); //different library, not sure if different, use if there are problems
						}
						catch (Exception ex)
						{
							//failed to make image, handle exception here
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, "Failed to Open Image");
							return;
						}
						
						//new image, set all globals to blank
						coords = null;
						date = null;
						drawLocation = 0;

						getExif(file);
						
						//fix image rotation
						image = imageRotate(image,direction);
						original = imageRotate(original,direction);

						drawLocation = image.getHeight()*0.97 - (3*1.1*70);//where the line of text should be, start at 97% mark - 3 lines at 70 pt font
						drawLocation = drawText(image,date,drawLocation); //add date to image
						drawLocation = drawText(image,coords,drawLocation); //add coords to image

						//draw image to ui
						//lblImage.getGraphics().drawImage(image, 0, 0, lblImage.getHeight(), lblImage.getWidth(), 0, 0, image.getHeight(), image.getWidth(),null);
						//decide if the image aspect ratio is wider or taller than label to decide how to scale it
						double ratioLabel = lblImage.getHeight()/lblImage.getWidth();
						if(chckbxLocation.isSelected()) {//are we displaying the gps data on image?
							double ratioImage = image.getHeight()/image.getWidth();
							if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
								lblImage.setIcon(new ImageIcon(image.getScaledInstance(lblImage.getWidth(), -1, 0)));
							}else {
								lblImage.setIcon(new ImageIcon(image.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
							}
						}else {
							double ratioImage = original.getHeight()/original.getWidth();
							if(ratioLabel>ratioImage) { //check if the label is relatively taller than the source image, if it is then shrink width to fit and scale the height to match
								lblImage.setIcon(new ImageIcon(original.getScaledInstance(lblImage.getWidth(), -1, 0)));
							}else {
								lblImage.setIcon(new ImageIcon(original.getScaledInstance(-1, lblImage.getHeight(), 0))); //set height to label and then scale width
							}
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
		
		JLabel lblNotes = new JLabel("Note:");
		lblNotes.setToolTipText("Add a short note to the bottom of the image");
		lblNotes.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblNotes.setBounds(7, 630, 40, 23);
		frame.getContentPane().add(lblNotes);

	}
	private void getExif(File imageFile) {
		outputSet = null;//reset old exif data before fetching
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
		if(metadata == null) {
			JOptionPane.showMessageDialog(null, "Error No Metadata");
			return;
		}
		final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		final TiffImageMetadata exif = jpegMetadata.getExif();
		
		//get orientation data so we can correct image if needed
		direction = 0;
		try {
			TiffField orientation = exif.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			if(orientation != null) {
				direction = orientation.getIntValue();
			}
		} catch (ImageReadException e) {
			e.printStackTrace();
			//no need to throw up message if orientation tag is missing, it should be fine.
		}

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
			coords = df.format(Math.abs(gpsInfo.getLatitudeAsDegreesNorth())) + "� " + gpsInfo.latitudeRef + ", " + df.format(Math.abs(gpsInfo.getLongitudeAsDegreesEast())) + "� " + gpsInfo.longitudeRef;
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
			//since we will be manually flipping the image and restoring the image exif, we need to update this tag to 1 so it does not get rotated by other programs
			if(outputSet != null && (direction != 0 || direction != 1)) {//check that the direction is not already "normal" and that we don't have empty metadata
				//final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
				//exifDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
				//exifDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) 1);
				outputSet.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			}
		} catch (ImageWriteException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to Copy Metadata, Cannot Save Metadata with New Image");
		}

		//print gps and date info to SYSO for debugging reasons, remove in final release. 
		//      System.out.println(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
		//      System.out.println(date);
		//      System.out.println(gpsInfo.latitudeDegrees + "� " + gpsInfo.latitudeMinutes + "\' " + gpsInfo.latitudeSeconds.doubleValue() + "\" " + gpsInfo.latitudeRef);
		//      System.out.println(gpsInfo.longitudeDegrees + "� " + gpsInfo.longitudeMinutes + "\' " + gpsInfo.longitudeSeconds.doubleValue() + "\" " + gpsInfo.longitudeRef);
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
		if(text == null) {//no text, skip rendering attempt
			return location;
		}
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
		graphic.dispose();
		return location;
	}

	//simple helper that takes a file and a set of EXIF data and rewrites the image file with the EXIF
	private void writeExif(File dest, TiffOutputSet tiffOutput ) {
		if(tiffOutput == null) {//no exif present, dont write any
			return;
		}
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
	
	//simple tool to rotate images to correct for image sensors that were rotated at the time of image taking
	private BufferedImage imageRotate(BufferedImage image, int exifDirection) {
		AffineTransform transform = new AffineTransform();//tool for defining how the image needs to be changed
		BufferedImage temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());//by defualt the same size as the original, change dimensions if rotating
		switch (exifDirection) {
		case 0:
			temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			break;//value was not set, image does not need rotating.
		case 1:
			temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			break;//Corresponds to the "normal" orientation
		case 2:
			 //mirrored in X, 0,0 is top right side transform.scale(-1.0, 1.0);
			 transform.translate(-image.getWidth(), 0);
			 temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			break;
		case 3:
			 //upside down, rotate 180. 0,0 is bottom right
			 transform.translate(image.getWidth(), image.getHeight());
			 transform.rotate(Math.PI);
			 temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			//g.rotate(Math.PI);
			//g.translate(temp.getWidth(), temp.getHeight());
			break;
		case 4:
			 //mirrored in Y, 0,0 is bottom left 
			 transform.scale(1.0, -1.0);
			 transform.translate(0, -image.getHeight());
			 temp = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			break;
		case 5:
			 //mirrored in X and rotated, 0,0 is left,top 
			 transform.rotate(-Math.PI/2);
			 transform.scale(-1.0, 1.0);
			 temp = new BufferedImage(image.getHeight(),image.getWidth(),image.getType());
			break;
		case 6:
			  //rotated counter clockwise 90, 0,0 is right,top
			  transform.translate(image.getHeight(), 0); 
			  transform.rotate(Math.PI/2);
			  temp = new BufferedImage(image.getHeight(),image.getWidth(),image.getType());
			//g.translate(temp.getWidth()/2, 0);
			//g.rotate(Math.PI/2);
			break;
		case 7:
			 //mirrored Y and rotated 0,0 is right,bottom 
			 transform.scale(-1.0, 1.0);
			 transform.translate(-image.getHeight(), 0); 
			 transform.translate(0,image.getWidth()); 
			 transform.rotate(3*Math.PI/2);
			 temp = new BufferedImage(image.getHeight(),image.getWidth(),image.getType());
			break;
		case 8:
			  //rotated clockwise 90 0,0 is left,bottom 
			 transform.translate(0,
			 image.getWidth()); 
			 transform.rotate(3*Math.PI/2);
			 temp = new BufferedImage(image.getHeight(),image.getWidth(),image.getType());
			//g.rotate(3*Math.PI/2);
			//g.translate(0, temp.getHeight());
			break;
		}
		
//		  AffineTransformOp op = new AffineTransformOp(transform,AffineTransformOp.TYPE_BICUBIC); 
//		  BufferedImage	temp = op.createCompatibleDestImage(image, image.getColorModel()); 
//		  Graphics2D g = temp.createGraphics(); 
//		  g.setBackground(Color.WHITE);
//		  g.clearRect(0, 0, temp.getWidth(), temp.getHeight()); temp = op.filter(image, temp);
//		  image=temp;
		Graphics2D g = temp.createGraphics();
		g.transform(transform);
		g.drawImage(image, null, 0, 0);
		g.dispose();
		return temp;
	}
	private BufferedImage bytecopy(BufferedImage Source) {
		java.awt.image.ColorModel ColorModel = Source.getColorModel();
		WritableRaster WritableRaster = Source.copyData(Source.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(ColorModel, WritableRaster, ColorModel.isAlphaPremultiplied(), null);
	}
}
