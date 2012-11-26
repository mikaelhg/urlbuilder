package gumi.builders.url;

package ca.bcpra.report.util.url;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.DatatypeConverter;


/**
 * Regular expressions for URL parsing.
 * <p>
 * There are times when I wish Java had such advanced features as multiline
 * strings, or even a zero-dependency command to read a file into a string.
 * <p>
 * @author Mikael Gueck gumi{@literal @}iki.fi
 */
public class UrlRegularExpressions {
  public static final Pattern RFC3986_GENERIC_URL;

  static {
    RFC3986_GENERIC_URL =
        Pattern.compile( decode( "Hz8IAAAAAAAAAD8/P04/QBA/Pz8pP1Q/Pz96JT9qCzFiTD9BDF4/Nj8/Aj8/Pz8tPyE+Pz9tPz8CNj9mPz98cz9nNWg/YRoFAm4+Jz8lST8/BD85fz8/SzQ/Vj8AP0EUPwMZP0M/ZTETYQ8/Pz86PzV4Pz8RPz8kCz9OYH0/Pzg/P01qP182HD9BP3oqR1w/cgYlPwRyRABsGw8/CRNuIT9WPz8/Pz90PxtlPz8/PzdrPwdrPwZtVD8/EU8/PzdhPz9DMGZ9UDokPz8/PwEyPz9vUn83Pzk9Pw0/FgR/WyU/IhsFPz8/VD8FBQs/cCFdPz8AP2Y/fz9oP2I/BD9TVz8/Jj8hP2x7Rj8/JjA/Az8IcTY/P2AxPxw/PzQDPz8uMD80Pw8fHgE/Pz8WP3A/P0w/FT8xPzwrcD8/Y2QrJlg/eSE/Pz8/P20/MD9+M1knPz9HP1wSPz9MOz8/Pz8/MjEjPz9dBS4/Pys/Pws/IT8/P1I/BkU/Um0/Pz8/H3M/PzY/AwAA" ),
                         Pattern.CASE_INSENSITIVE | Pattern.COMMENTS );
  }

  /**
   * Decodes a string that is Base64 encoded and zipped. This is called only
   * once.
   * 
   * @param s The string to decode.
   * @return The decoded string, or s on any cosmic exceptions.
   */
  private static String decode( String s ) {
    byte[] b = DatatypeConverter.parseBase64Binary( s );
    StringBuilder sb = new StringBuilder( 1024 );

    try {
      ByteArrayInputStream bais = new ByteArrayInputStream( b );
      GZIPInputStream gzis = new GZIPInputStream( bais );
      InputStreamReader reader = new InputStreamReader( gzis, "UTF-8" );
      BufferedReader in = new BufferedReader( reader );
      String line = in.readLine();

      while ( line != null ) {
        sb.append( line );
        line = in.readLine();
      }
    } catch ( IOException e ) {
      // Should only happen if... http://lambda-diode.com/opinion/ecc-memory
      sb.setLength( 0 );
      sb.append( s );
    }

    return sb.toString();
  }
}
