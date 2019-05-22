import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SeamCarv
{
	
	enum Opertion
	{
		Pmn,H;
	}
	
	public static boolean isValid (int height, int width, int i, int j)
	{
		return (i >= 0 && j >= 0 && i < width && j < height);
	}
	
	
	public static double CellCalc (double [][] map, int i, int j,Opertion operation)
	{
		double sum = 0;
		int height = map[0].length;
		int width = map.length;
		
		for (int k=-4 ; k<5 ; k++)
		{
			for (int m=-4 ; m<5 ; m++)
			{
				if (isValid(height, width, i+k, j+m))
				{
					if(operation==Opertion.Pmn) 
						sum+=map[i+k][j+m];
					else
						sum+=map[i+k][j+m]*Math.log(map[i+k][j+m]);
				}
			}
		}
		
		if(sum==0)
		{
			return 0;
		}
		
		return (operation==Opertion.Pmn)? map[i][j]/sum : -sum;
	}
	
 	public static double [][] HMap(BufferedImage image)
	{
		double[][] greyScaleMap = imageToGreyScaleMap(image);
		int height = greyScaleMap[0].length;
		int width = greyScaleMap.length;
		double [][] PmnMap = new double [width][height];
		
		for (int i=0 ; i<width ; i++)
		{
			for (int j=0 ; j<height ; j++)
			{
				PmnMap[i][j] = CellCalc(greyScaleMap, i, j,Opertion.Pmn);
			}
		}
		
		for (int i=0 ; i<width ; i++)
		{
			for (int j=0 ; j<height ; j++)
			{
				greyScaleMap[i][j] = CellCalc(PmnMap, i, j,Opertion.H);
			}
		}
		
		return greyScaleMap;
	}
	
	public static double [][] imageToGreyScaleMap(BufferedImage image)
	{
		Color rgbOrigin;
		int height = image.getHeight();
		int width = image.getWidth();
		double [][] greyScaleMap = new double [width][ height];
			
		for (int i=0 ; i<width ; i++)
		{
			for (int j=0 ; j<height ; j++)
			{
				rgbOrigin = new Color(image.getRGB(i, j));
				int mean = (int) (rgbOrigin.getRed()+rgbOrigin.getGreen()+rgbOrigin.getBlue())/3;
				greyScaleMap[i][j] = mean; //col.getRGB();
			}
		}
		
		return greyScaleMap;	
	}
	
	public static double CellenergyCalc (BufferedImage image, int i, int j)
	{
		Color rgbOrigin = new Color(image.getRGB(i, j));
		int redOrigin = rgbOrigin.getRed();
		int greenOrigin = rgbOrigin.getGreen();
		int blueOrigin = rgbOrigin.getBlue();
		
		int redTemp, greenTemp, blueTemp;
		Color rgbTemp;
		
		double sum=0;
		int cnt = 0;
		int height = image.getHeight();
		int width = image.getWidth();
		
		for (int k=-1 ; k<2 ; k++)
		{
			for (int m=-1 ; m<2 ; m++)
			{
				if (!(k==0 && m==0) && isValid(height, width, i+k, j+m))
				{
					cnt++;
					rgbTemp = new Color(image.getRGB(i+k, j+m));
					
					redTemp = rgbTemp.getRed();
					greenTemp = rgbTemp.getGreen();
					blueTemp = rgbTemp.getBlue();
					sum+=(Math.abs(redOrigin-redTemp)+Math.abs(greenOrigin-greenTemp)+Math.abs(blueOrigin-blueTemp))/3;
				}
			}
		}
		return sum/cnt;
	}
	
	public static double[][] energyMapCalc(BufferedImage image)
	{
		int height = image.getHeight();
		int width = image.getWidth();
		double [][] energyMap = new double [width][ height];
		for (int i=0 ; i<width ; i++)
		{
			for (int j=0 ; j<height ; j++)
			{
				energyMap[i][j] = CellenergyCalc(image, i, j);
			}
		}
		return energyMap;
		
	}
	
	public static double[][] FinalMap(BufferedImage image)
	{
		double [][] energyMap = energyMapCalc(image);
		double [][] HMap = HMap(image);
		int height = image.getHeight();
		int width = image.getWidth();
		for(int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				HMap[i][j] =  HMap[i][j] * 0.01 + energyMap[i][j] * 0.99;  //col.getRGB();
			}
				
		}
		return HMap;
	}
	
	public static int cloumnCalc(double [][] Map, int index)
	{
		int height = Map[0].length;
		int sum = 0;

		for (int i = 0; i < height; i++)
		{
			sum += Map[index][i];
		}
		return sum;
	}
	
	public static BufferedImage rotateCw( BufferedImage img )
	{
		int width = img.getWidth();
		int height = img.getHeight();
	    BufferedImage   newImage = new BufferedImage( height, width, img.getType() );

	    for( int i=0 ; i < width ; i++ )
	        for( int j=0 ; j < height ; j++ )
	            newImage.setRGB( height-1-j, i, img.getRGB(i,j) );

	    return newImage;
	}
	
	public static int FindIndexOfStraightSeam(BufferedImage image, int whichMap)
	{	
		
		double [][] Map;
		
		if (whichMap == 0) Map = energyMapCalc(image);
		else if (whichMap == 1)  Map = FinalMap(image);
		else Map = forwardEnergyMap(image);
		
		int minEnergySeam = cloumnCalc(Map, 0);
		int minEnergyIndex = 0;
		int minEnergyTemp = 0;
		
		
		for(int i = 1; i< Map.length; i++)
		{
			minEnergyTemp = cloumnCalc(Map, i);
			
			if(minEnergyTemp<minEnergySeam)
			{
				minEnergySeam=minEnergyTemp;
				minEnergyIndex=i;
			}
		}
		
		return minEnergyIndex;
	}
	
	public static BufferedImage deleteStraightMinSeam(BufferedImage image, int whichMap)
	{
		int index = FindIndexOfStraightSeam(image, whichMap);
		
		BufferedImage newImage = new BufferedImage(image.getWidth()-1, image.getHeight(), image.getType());
		
		for (int i=0 ;i<image.getWidth()-1; i++)
		{
			for (int j=0; j<image.getHeight(); j++)
			{
				if (i>=index)
				{
					newImage.setRGB(i, j, image.getRGB(i+1, j));
				}
				else 
				{
					newImage.setRGB(i, j, image.getRGB(i, j));
				}
			}
		}
		
		return newImage;
	}
	
	public static BufferedImage duplicateStraightSeamByIndex(BufferedImage image, int index)
	{
		int height = image.getHeight();
		int width = image.getWidth();
		BufferedImage newImage = new BufferedImage(width+1, height, image.getType());
		
		for (int i=0 ;i<width+1; i++)
		{
			for (int j=0; j<height; j++)
			{
				if (i>index)
				{
					newImage.setRGB(i, j, image.getRGB(i-1, j));
				}
				else 
				{
					newImage.setRGB(i, j, image.getRGB(i, j));
				}
			}
		}
		
		return newImage;
	}
	
	public static BufferedImage duplicateStraightMinSeam(BufferedImage image, int whichMap)
	{
		int index = FindIndexOfStraightSeam(image, whichMap);
		return duplicateStraightSeamByIndex(image, index);
	}
	
	public static boolean[] SelectStrSeam(BufferedImage image, boolean[] minSeamArr, int whichMap)
	{
		double [][] Map;
		
		if (whichMap == 0) Map = energyMapCalc(image);
		else if (whichMap == 1)  Map = FinalMap(image);
		else Map = forwardEnergyMap(image);
		
		int minEnergySeam = cloumnCalc(Map, 0);
		int minEnergyIndex = 0;
		int minEnergyTemp = 0;
		int cnt = 0;
		while(cnt < minSeamArr.length)
		{
			if(minSeamArr[cnt] == false)
			{
				minEnergySeam = cloumnCalc(Map, cnt);
				minEnergyIndex = cnt;
				break;
			}
			cnt++;
		}
		
		
		for(int i = 0; i< Map.length; i++)
		{
			if(minSeamArr[i] == false)
			{
				minEnergyTemp = cloumnCalc(Map, i);
				
				if(minEnergyTemp<minEnergySeam)
				{
					minEnergySeam=minEnergyTemp;
					minEnergyIndex=i;
				}
			}			
		}
		minSeamArr[minEnergyIndex] = true;
		return minSeamArr;
	}

	public static BufferedImage DuplicateMinKStraightSeams(BufferedImage image, int k, boolean half, int whichMap)
	{
		BufferedImage newImage  = image;	
		int width = image.getWidth();
		boolean [] minSeamArr = new boolean [width]; 
		int cnt = 0;
		int changed = 0;
		for(int i = 0; i < width; i++)
		{
			minSeamArr[i] = false;
		}
		while (cnt < k)
		{
			minSeamArr = SelectStrSeam(image, minSeamArr, whichMap);
			cnt ++;
		}
		for(int i = 0; i < width; i++)
		{
			if(minSeamArr[i] == true)
			{
				newImage = duplicateStraightSeamByIndex(newImage, i + changed);
				changed ++;
			}
		}
		return newImage;
	}
	
	public static BufferedImage DuplicateMinKStraightSeams(BufferedImage image, int k, int whichMap)
	{
		int howMany = k/(image.getWidth()/2);
		int cnt = 0;
		BufferedImage newImage  = image;	
		if(k <= image.getWidth()/2)
		{
			newImage = DuplicateMinKStraightSeams(image, k, true, whichMap);
		}
		else 
		{
			while(cnt < howMany)
			{
				newImage = DuplicateMinKStraightSeams(newImage, image.getWidth()/2, true, whichMap);
				cnt++;
			}
			if(k > (newImage.getWidth()-image.getWidth()))
				newImage = DuplicateMinKStraightSeams(newImage, k - (newImage.getWidth()-image.getWidth()) , true, whichMap);
			
		}
		return newImage;	
	}
	
	public static BufferedImage rotateClockwise90(BufferedImage src)
	{
	    int w = src.getWidth();
	    int h = src.getHeight();
	    BufferedImage dest = new BufferedImage(h, w, src.getType());
	    for (int y = 0; y < h; y++) 
	        for (int x = 0; x < w; x++) 
	            dest.setRGB(y, w - x - 1, src.getRGB(x, y));
	    return dest;
	}
	
	public static BufferedImage deleteSeamByArrayOfIndexes(BufferedImage image, int [] indexes)
	{
		int height = image.getHeight();
		int width = image.getWidth();
		BufferedImage newImage = new BufferedImage(width-1, height, image.getType());
		
		if (height!= indexes.length)
		{
			System.out.println("Invalid date!");
			return null;
		}
		
		for (int i=0 ;i<width-1; i++)
		{
			for (int j=0; j<height; j++)
			{
				if (i>=indexes[j])
				{
					newImage.setRGB(i, j, image.getRGB(i+1, j));
				}
				else 
				{
					newImage.setRGB(i, j, image.getRGB(i, j));
				}
			}
		}
		
		return newImage;
	}
	
	public static int interpolate(int RGB1, int RGB2)
	{
		Color c1 = new Color(RGB1);
		Color c2 = new Color(RGB2);
		
		int r = (c1.getRed() + c2.getRed()) / 2;
		int g = (c1.getGreen() + c2.getGreen()) / 2;
		int b = (c1.getBlue() + c2.getBlue()) / 2;
		
		return new Color(r,g,b).getRGB();
	}
	
	public static BufferedImage duplicateSeamByArrayOfIndexes(BufferedImage image, int [] indexes)
	{
		int height = image.getHeight();
		int width = image.getWidth();
		BufferedImage newImage = new BufferedImage(width+1, height, image.getType());
		
		if (height!= indexes.length)
		{
			System.out.println("Invalid date!");
			return null;
		}
		
		for (int i=0 ;i<width+1; i++)
		{
			for (int j=0; j<height; j++)
			{
				if (i>indexes[j])
				{
					if((i == indexes[j] + 1))
					{
						int direction = (i == width) ? -2 : 0;
						newImage.setRGB(i, j, interpolate((image.getRGB(i-1, j)),image.getRGB(i+direction, j)));
					}
						
					else
					{
						newImage.setRGB(i, j, image.getRGB(i-1, j));
					}
						
				}
				else 
				{
					newImage.setRGB(i, j, image.getRGB(i, j));
				}
			}
		}
		
		return newImage;
	}
	
	public static int [][] findKDiagSeamByMatrixArrayOfIndexes (BufferedImage image, int k, int whichMap)
	{
		int [][] indexes = new int[k][];
		double [][] Map;
		
		if (whichMap == 0) Map = energyMapCalc(image);
		else if (whichMap == 1)  Map = FinalMap(image);
		else Map = forwardEnergyMap(image);
		
		for(int i=0; i<k; i++)
		{
			indexes[i]=FindMinDiagonalSeam(image, Map);
			addEnergyToSeam(indexes[i], Map);
		}
		return indexes;
	}
	
	public static BufferedImage DeleteMinKDiagonalSeam(BufferedImage image, int k, int whichMap)
	{
		int [] path = new int[image.getHeight()];
		double [][] Map;
		
		if (whichMap == 0) Map = energyMapCalc(image);
		else if (whichMap == 1)  Map = FinalMap(image);
		else Map = forwardEnergyMap(image);
		
		for(int i=0; i<k; i++)
		{
			System.out.println(i);
			path = FindMinDiagonalSeam(image, Map);
			image = deleteSeamByArrayOfIndexes(image, path);
			Map=FinalMap(image);
		}
		
		return image;
	}
	
	public static BufferedImage DuplicateKSeamsDiag(BufferedImage image, int k, boolean half, int whichMap)
	{
		int [][] seams = findKDiagSeamByMatrixArrayOfIndexes(image, k, whichMap);
		for(int i=0; i<k ; i++)
		{
			image = duplicateSeamByArrayOfIndexes(image,seams[i]);
			for (int j=i ; j<k; j++)
			{
				System.out.println(i);
				for (int m=0; m<image.getHeight(); m++)
				{
					if(seams[i][m]<=seams[j][m])
					{
						seams[j][m]+=1;
					}
				}
			}
		}
		return image;
	}
	
	public static BufferedImage DuplicateKSeamsDiag(BufferedImage image, int k, int whichMap)
	{
		int howMany = k/(image.getWidth()/2);
		int cnt = 0;
		BufferedImage newImage  = image;	
		if(k <= image.getWidth()/2)
		{
			newImage = DuplicateKSeamsDiag(image, k, true, whichMap);
		}
		else 
		{
			while(cnt < howMany)
			{
				System.out.println(cnt);
				newImage = DuplicateKSeamsDiag(newImage, image.getWidth()/2, true, whichMap);
				cnt++;
			}
			if(k > (newImage.getWidth()-image.getWidth()))
				newImage = DuplicateKSeamsDiag(newImage, k - (newImage.getWidth()-image.getWidth()) , true, whichMap);
			
		}
		return newImage;
	}
	
	public static int[] FindMinDiagonalSeam(BufferedImage image, double[][] finalMap)
	{
		int height = image.getHeight();
		int width = image.getWidth();
		double[][] copymap = new double[width][height];
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				copymap[i][j] = finalMap[i][j];
			}
		}
		
		for (int j = 1; j < height; j++)
		{
			for (int i = 0; i < width; i++)
			{
				double min = copymap[i][j - 1];
				for (int k = -1; k < 2; k++)
				{
					if (k != 0 && i + k >= 0 && i + k < width && copymap[i + k][j - 1] < min)
					{
						min = copymap[i + k][j - 1];
					}
				}
				copymap[i][j] += min;
			}
		}
		
		int col = startPoint(copymap);
		int[] path = new int[height];
		int row = height - 1;
		path[row--] = col;
		while (row >= 0)
		{
			int nextCol = col;
			double tmpVal = copymap[col][row];
			for (int l = -1; l < 2; l += 1)
			{
				if (l != 0 && isValid(height, width, col + l, row) && copymap[col + l][row] < tmpVal)
				{
					tmpVal = copymap[col + l][row];
					nextCol = col + l;
				}
			}
			path[row--] = nextCol;
			col = nextCol;
		}

		return path;
	}

	private static int startPoint(double[][] Map)
	{
			int j = Map[0].length-1;
			double tmp = Map[0][j];
			int minCol = 0;
			for (int i = 1 ; i < Map.length;i++) {
				if (tmp > Map[i][j]) {
					tmp = Map[i][j];
					minCol = i;
				}
			}
			return minCol;
	}
		
	private static void addEnergyToSeam(int[] path,double[][] Map)
	{
		for (int i = 0 ; i < Map[0].length ; i++)
		{
			Map[path[i]][i]+=1;
		}
	}
	
	public static double[][] forwardEnergyMap(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		double cU = 0, cL = 0, cR = 0, mU = 0, mD = 0, mL = 0;
		int up = 0, left = 0, down = 0;
		double [][]I = imageToGreyScaleMap(image);
		double [][] map = new double [width][height];
		double [][]M = new double [width][height];
		for(int j = height - 1; j > 0; j--) {
			for(int i = 0; i < width; i++) {
				up = (j - 1) ;
				down = (j + 1);
				left = (i - 1);
				cU = down < height ? Math.abs(I[i][up] - I[i][down]): Integer.MAX_VALUE;
				cL = down < height && left >= 0 ? (Math.abs(I[left][j] - I[i][down]) + cU) : Integer.MAX_VALUE;
				cR = left >= 0 ? (Math.abs(I[left][j] - I[i][up]) + cU) : Integer.MAX_VALUE;
				mU = left >= 0 ? M[left][up] : Double.MAX_VALUE;
				mD = down < height && left >= 0 ? M[left][down] : Double.MAX_VALUE;
				mL = left >= 0 ? M[left][j] : Double.MAX_VALUE;
				M[i][j] = Math.min((mU + cL), Math.min((mL + cU), (mD + cR)));
				map[i][j] = M[i][j];
			}
			
		}
		return map;
		
	}
	
	public static void overall (String inputPath, int columns, int rows, int whichMap, String outputPath) throws IOException
	{
		File file= new File(inputPath);
		BufferedImage image = ImageIO.read(file);
		
		int originalHeight = image.getHeight();
		int originalWidth = image.getWidth();
		
		int differenceHeight = rows - originalHeight;
		int differenceWidth = columns - originalWidth;
		
		if (differenceHeight>0 && differenceWidth>0)
		{
			image = DuplicateKSeamsDiag(image, differenceWidth, whichMap);
			image = rotateCw(image);
			image = DuplicateKSeamsDiag(image, differenceHeight, whichMap);
		}
		
		else if (differenceHeight<=0 && differenceWidth<=0)
		{
			image = DeleteMinKDiagonalSeam(image, (-differenceWidth), whichMap);
			image = rotateCw(image);
			image = DeleteMinKDiagonalSeam(image, (-differenceHeight), whichMap);
		}
		
		else if (differenceHeight>0 && differenceWidth<=0)
		{
			image = DeleteMinKDiagonalSeam(image, (-differenceWidth), whichMap);
			image = rotateCw(image);
			image = DuplicateKSeamsDiag(image, differenceHeight, whichMap);
		}
		
		else if (differenceHeight<=0 && differenceWidth>0)
		{
			image = DuplicateKSeamsDiag(image, differenceWidth, whichMap);
			image = rotateCw(image);
			image = DeleteMinKDiagonalSeam(image, (-differenceHeight), whichMap);
		}
		
		image = rotateClockwise90(image);
		
		File outputFile = new File(outputPath);
		ImageIO.write(image, "bmp", outputFile);
		
	}
	
	public static void main(String[] args) throws IOException
	{
		String inputPath = args[0];
		int columns = Integer.parseInt(args[1]);
		int rows = Integer.parseInt(args[2]);
		int whichMap = Integer.parseInt(args[3]); //0: energyMap, 1: finalMap, 2: forwardMap
		String outputPath = args[4];
		
		SeamCarv.overall(inputPath, columns, rows, whichMap, outputPath);
	}

}
