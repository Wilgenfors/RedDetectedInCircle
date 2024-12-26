package TargetRecognize;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RedMain {
	private static JFrame mainFrame;
	static BufferedImage myPicture = null;
	 private static Timer timer;
	 static String fileName = "";
	 static ArrayList<Circle> circlesList;
	 
	public static void main(String[] args) {
//		consoleTest();
		guiTest();
	}

// Работа с файлами:
	public static List<String> listFilesUsingJavaIO(String dir) { //для получения списка файлов .png и .jpg в каталоге
	    return Stream.of(new File(dir).listFiles())
	      .filter(file -> !file.isDirectory() && (file.getPath().contains(".png") || file.getPath().contains(".jpg")))
	      .map(File::getName)
	      .collect(Collectors.toList());
	}
	
	private static JList<String> getListWithFiles() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		listModel.addAll(listFilesUsingJavaIO(System.getProperty("user.dir")));
		System.out.println("listModel = "+listModel);
		JList<String> filesList = new JList<>(listModel);
		return filesList;
	}
	
	private static void guiTest() {

		mainFrame = new JFrame("RedTargetTest");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MyLabel imageLabel = new MyLabel();
		JList<String> filesList = getListWithFiles();
		fileName = filesList.getModel().getElementAt(0);   
		try {
			myPicture = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		ImageIcon imgIcon = new ImageIcon(myPicture);
		imageLabel.setIcon(imgIcon);
		mainFrame.add(imageLabel, BorderLayout.CENTER);
		mainFrame.add(filesList, BorderLayout.EAST);
		mainFrame.setSize(800, 600);
		mainFrame.setVisible(true);
		resizeImage(imageLabel, myPicture, imgIcon);
		mainFrame.setLocationRelativeTo(null);
		filesList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				JList tempList = (JList) e.getSource();
				fileName = (String) tempList.getSelectedValue();
				System.out.println("fileName = "+ fileName);
				try {
					myPicture = ImageIO.read(new File(fileName));
				} catch (IOException err) {
					err.printStackTrace();
				}
				resizeImage(imageLabel, myPicture, imgIcon);
				mainFrame.invalidate();
			}
		});
		timer = new Timer(50, e -> {
			System.out.println("Resize action performed!");
			resizeImage(imageLabel, myPicture, imgIcon);
		});
		timer.setRepeats(false); // Only execute once after resizing stops

//		mainFrame.addComponentListener(new ComponentAdapter() {
//			@Override
//			public void componentResized(ComponentEvent e) {
//				timer.restart();
////				System.out.println("--!!resized!!--");
//			}
//		});
		imageLabel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				timer.restart();
				resizeImage(imageLabel, myPicture, imgIcon);
			}
		});


		// Для того чтобы список не был пустой:
		SearchCircleAndPoint redSearch = new SearchCircleAndPoint(myPicture);
		Circle circle = redSearch.getCircle(); //находим внешний круг
		ArrayList<Circle> circlesList = redSearch.getCircles(circle); //находим все внутренние круги
		imageLabel.drawCircles(circlesList);
		circlesList.add(circle); //если нужен список со всеми кругами
		// ---------------------------------------------------------------

//		var myCircle = detectedRedPointOnTarget(imageLabel, myPicture, imgIcon);
//		checkTarget(circlesList, myCircle);
		
	}

	private static void resizeImage(MyLabel imageLabel, BufferedImage myPicture, ImageIcon imgIcon) {
		float dHeight = imageLabel.getHeight() / (float) myPicture.getHeight();
		int newWidth = (int) (myPicture.getWidth() * dHeight);
		Image dimg = myPicture.getScaledInstance(newWidth, imageLabel.getHeight(), Image.SCALE_SMOOTH);
		imgIcon.setImage(dimg);
		SearchCircleAndPoint redSearch = new SearchCircleAndPoint(myPicture);

		redSearch.findRedPoints(); //Находим координаты красной точки
		Circle circle = redSearch.getCircle(); //находим внешний круг
		if (circle==null) {
			System.out.println("--!! No circle !!--");
		} else {
			
			//red point detected:
			// Объектная переменная для синей обводки:
			 Circle myPoint = detectedRedPointOnTarget(imageLabel, myPicture, imgIcon);
		        imageLabel.drawPoint(myPoint, dHeight);
			//--------------------------------------------------------------------
			imageLabel.drawCircle(circle.getX(), circle.getY(), circle.getRadius(), dHeight);
			System.out.println("--- inner circles search ---");
			ArrayList<Circle> circlesList = redSearch.getCircles(circle); //находим все внутренние круги
			imageLabel.drawCircles(circlesList);
			circlesList.add(circle); //если нужен список со всеми кругами

			// Новый алгоритм точности попадания:
			int circleIndex = getCircleIndByXY(myPoint.getX(), myPoint.getY(), circlesList);
			if (circleIndex==0) System.out.println("Red point detected in center.");
			else if (circleIndex > circlesList.size()) System.out.println("Red point detected with miss.");
			else System.out.println("Red point detected between "+(circleIndex-1)+" and "+(circleIndex+1));
			// ----------------------------------------------------------------------
			
		}
	}

	// Методы для алгоритма точности попадания:
	private static int getCircleIndByXY(int xRedPoint, int yRedPoint, ArrayList<Circle> circles2) {
		int i = 0;
		for (Circle circle : circles2) {
			int count = circles2.indexOf(circle);
			// Проверяем чтобы следующий круг не выходил за границы:
			if ((count) <circles2.size()-1 ){
				Circle circleNext = circles2.get(count + 1);
				if (circleIs_OnXY(circle, circleNext, xRedPoint, yRedPoint)) return i;
				i++;
			}
		}
		return -1;
	}

	private static boolean circleIs_OnXY(Circle circle,Circle circleNext, int xRedPoint, int yRedPoint) {
//		(x – a)2 + (y – b)2 = R2
		int circleRc1 = circle.getRadius();
		int circleRc2 = circleNext.getRadius();
		int thick = circle.getThickness();
		// Левая часть нужна для нахождения радиуса от центра до нашей красной точки:
		double leftPart_1 = Math.pow(xRedPoint-circle.getX(), 2) + Math.pow(yRedPoint-circle.getY(), 2);
		double leftPart_2 = Math.pow(xRedPoint-circleNext.getX(), 2) + Math.pow(yRedPoint-circleNext.getY(), 2);
		// И сравниваем с радиусами текущего и следующего круга:
		if (leftPart_1>=((circleRc1-thick)*(circleRc1-thick)) && leftPart_2<=((circleRc2+thick)*(circleRc2+thick)))
			return true;
		return false;
	}
	//__________________________________________________________________________________________________________________




// Метод добaвленный из IDEA:
	// metod for detected red poins on target:
    private static Circle detectedRedPointOnTarget(MyLabel imageLabel, BufferedImage myPicture, ImageIcon imgIcon) {
        float dHeight = imageLabel.getHeight() / (float) myPicture.getHeight();
        int newWidth = (int) (myPicture.getWidth() * dHeight);
        Image dimg = myPicture.getScaledInstance(newWidth, imageLabel.getHeight(), Image.SCALE_SMOOTH);
        imgIcon.setImage(dimg);

		SearchCircleAndPoint redSearch = new SearchCircleAndPoint(myPicture);

        Circle myPoint = redSearch.findRedPointsAsCircle(); // это наша красная точка

        imageLabel.drawPoint(myPoint, dHeight);//, обведенная синим квадратом

        System.out.println("red dot circle = " + myPoint.getX() + " " + myPoint.getY() + " " + myPoint.getRadius());
        return myPoint;


    }

//	// metod for detected red poins on target:
//	private static Circle detectedRedPointOnTarget(MyLabel imageLabel, BufferedImage myPicture, ImageIcon imgIcon,ArrayList<Circle> circlesList) {
//		System.out.println("metod detectedRedPointOnTarget");
//		float dHeight = imageLabel.getHeight() / (float) myPicture.getHeight();
//		int newWidth = (int) (myPicture.getWidth() * dHeight);
//		Image dimg = myPicture.getScaledInstance(newWidth, imageLabel.getHeight(), Image.SCALE_SMOOTH);
//		imgIcon.setImage(dimg);
//		SearchCircleAndPoint redSearch = new SearchCircleAndPoint(myPicture);
//
//		System.out.println("\ncircleList size = " + circlesList.size() + "\n");
//		Circle myCircle = redSearch.findRedPointsAsCircle(); // это наша красная точка, обведенная окружностью
//		imageLabel.drawPoint(myCircle, dHeight);
//		System.out.println("red dot circle = " + myCircle.getX() + " " + myCircle.getY() + " " + myCircle.getRadius());
//		return myCircle;
//	}

	// todo исправить алгоритм нахождения точности красной точки.
	// Проблема заключается в том что он находит красную точку только в центре
	// а во внешних кругах он видит как промах.

//	private static void checkTarget(ArrayList<Circle> circlesList, Circle myCircle) {
//		System.out.println("metod checkTarget");
//		System.out.println("Going through the target: ");
//		System.out.println("circles = "+circlesList.size());
//
//		boolean miss = false, between = false;
//		for (Circle circle0 : circlesList) {
//			int i = circlesList.indexOf(circle0);
//
//			Circle circle1 = circlesList.get(i + 1);
//
//			int Cx = circle0.getX();
//			int Cy = circle0.getY();
//			int R = circle0.getRadius();
//
//
//			// Проверяем на промах:
//			if (i < circlesList.size() - 1 && checkRedPointAndCircleOut(myCircle, circle0)) {
//				System.out.println("miss");
//				miss = true;
//				break;
//			}
//			// Проверяем окружности на попадания без внутренего круга и промаха:
//			if ( i < circlesList.size() - 1 && checkRedPointAndCircle(myCircle, circle0, circle1)) {
//				System.out.println("between " + i + " & " + (i + 1));
//				between = true;
//				break;
//			}
//			//  Проверяем на попадание во внутрению окружность:
//			if (circle1 == circlesList.get(2) && (!miss && !between)) {
//				System.out.println("in center");
//				break;
//			}
//		}
//	}
//
//	private static double getRadius(Circle myCircle, Circle circle0) {
//		return Math.pow((myCircle.getX() - circle0.getX()), 2) + Math.pow((myCircle.getY() - circle0.getY()), 2);
//	}
//
//	private static boolean checkRedPointAndCircleOut(Circle myCircle, Circle circle) {
//		if (getRadius(myCircle, circle) > Math.pow(circle.getRadius(), 2)) return true;
//		return false;
//	}
//
//	private static boolean checkRedPointAndCircleCentre(Circle myCircle, Circle circle) {
//		if (getRadius(myCircle, circle) < Math.pow(circle.getRadius(), 2)) return true;
//		return false;
//	}
//
//	private static boolean checkRedPointAndCircle(Circle myCircle, Circle circle0, Circle circle1) {
//		if (getRadius(myCircle, circle0) < Math.pow(circle0.getRadius(), 2)
//				&& getRadius(myCircle, circle1) > Math.pow(circle1.getRadius(), 2))
//			return true;
//		return false;
//	}

}
