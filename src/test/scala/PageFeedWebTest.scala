import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import _root_.org.apache.http.client.HttpClient
import net.gfxmonk.android.pagefeed._

class PagefeedWebTest extends Spec with ShouldMatchers with MockitoSugar {

	describe("PagefeedWeb") {

		val http = mock[HttpClient]
		val web = new PagefeedWeb(http)
		it("should parse a JSON response into Url objects") {
			var expectedUrls = List(
				Url.remote("http://example.com/1", 12345, "The best article"),
				Url.remote("http://example.com/2", 123456, "The second best article")
			)
			var json = """
				[
					{"date": 12345, "url": "http:\/\/example.com\/1", "title": "The best article"},
					{"date": 123456, "url": "http:\/\/example.com\/2", "title": "The second best article"},
				]
			"""
				
			web.parse(json) should equal(expectedUrls)
		}

	}
}

