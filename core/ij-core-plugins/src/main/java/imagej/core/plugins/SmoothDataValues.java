package imagej.core.plugins;

import imagej.model.Dataset;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;

/**
 * TODO
 * @author Barry DeZonia
 *
 * @param <T>
 */
@Plugin(
		menuPath = "Process>Smooth"
)
public class SmoothDataValues<T extends RealType<T>> extends ImglibOutputAlgorithmPlugin<T>
{
	@Parameter
	private Dataset in;
	
	private String errMessage;

	public SmoothDataValues()
	{
	}
	
	@Override
	public void run()
	{
		if (in == null)  // TODO - temporary code to test these until IJ2 plugins can correctly fill a Dataset @Parameter
		{
			Image<UnsignedShortType> junkImage = Dataset.createPlanarImage("", new UnsignedShortType(), new int[]{200,200});
			Cursor<UnsignedShortType> cursor = junkImage.createCursor();
			int index = 0;
			for (UnsignedShortType pixRef : cursor)
				pixRef.set((index++)%243);
			cursor.close();
			in = new Dataset(junkImage);
		}
		
		OutputAlgorithm<T> algorithm = new SmoothAlgorithm(in);
		
		setAlgorithm(algorithm);
		
		super.run();
	}
	
	/** implements IJ1's ImageProcessor::smooth() algorithm within the structures of imglib's OutputAlgorithm */
	private class SmoothAlgorithm implements OutputAlgorithm<T>
	{
		private Image<T> inputImage;
		private Image<T> outputImage;
		
		public SmoothAlgorithm(Dataset input)
		{
			inputImage = (Image<T>) input.getImage();
			outputImage = inputImage.createNewImage();
		}

		@Override
		public boolean checkInput()
		{
			if (inputImage == null)
			{
				errMessage = "input image is null";
				return false;
			}
			
			if (inputImage.getNumDimensions() != 2)
			{
				errMessage = "input image is not 2d but has "+inputImage.getNumDimensions()+" dimensions";
				return false;
			}
			
			return true;
		}

		@Override
		public String getErrorMessage()
		{
			return errMessage;
		}

		@Override
		public boolean process()
		{
			LocalizableByDimCursor<T> outputCursor = outputImage.createLocalizableByDimCursor();
			OutOfBoundsStrategyFactory<T> factory = new OutOfBoundsStrategyMirrorFactory<T>();
			LocalizableByDimCursor<T> inputCursor = inputImage.createLocalizableByDimCursor(factory);

			int[] inputPosition = new int[inputCursor.getNumDimensions()];
			int[] localInputPosition = new int[inputCursor.getNumDimensions()];
			
			while (outputCursor.hasNext())
			{
				T outputValue = outputCursor.next();
				
				outputCursor.getPosition(inputPosition);
				
				double sum = 0;

				for (int dy = -1; dy <= 1; dy++)
				{
					localInputPosition[1] = inputPosition[1] + dy;
					for (int dx = -1; dx <= 1; dx++)
					{
						localInputPosition[0] = inputPosition[0] + dx;
						inputCursor.setPosition(localInputPosition);
						sum += inputCursor.getType().getRealDouble();
					}
				}
				
				outputValue.setReal(sum / 9);
			}
			
			inputCursor.close();
			outputCursor.close();

			return true;
		}

		@Override
		public Image<T> getResult()
		{
			return outputImage;
		}
	}
}
