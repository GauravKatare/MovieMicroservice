package level1.moviecatalog.resource;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import level1.moviecatalog.models.CatalogItem;
import level1.moviecatalog.models.Movie;
import level1.moviecatalog.models.UserRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class CatalogResource {

    @Autowired
    RestTemplate restTemplate;

        @RequestMapping("/{userId}")
        @HystrixCommand(fallbackMethod="getfallbackCatalog",commandProperties ={ @HystrixProperty(name = "execution.isolation.thread.timeoutMilliseconds",value = "2000"),
                @HystrixProperty(name ="circuitBreaker.requestVolumeThreshold",value ="5"),
                @HystrixProperty(name ="circuitBreaker.errorThresholdPercentage" ,value ="50"),
                @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliSeconds",value = "5000")}
        )
        public List<CatalogItem> getCatalog(@PathVariable("userId") String userId) {
            UserRating userRating = restTemplate.getForObject("http://ratings-data-service/ratingsdata/user/" + userId, UserRating.class);

            return userRating.getRatings().stream()
                    .map(rating -> {
                        Movie movie = restTemplate.getForObject("http://movie-info-service/movies/" + rating.getMovieId(), Movie.class);
                        return new CatalogItem(movie.getName(), "Description", rating.getRating());
                    })
                    .collect(Collectors.toList());
        }

        public List<CatalogItem> getfallbackCatalog(@PathVariable("userId") String userId){
            return Arrays.asList(new CatalogItem("No movie","",0));
        }
    }

/*
Alternative WebClient way
Movie movie = webClientBuilder.build().get().uri("http://localhost:8082/movies/"+ rating.getMovieId())
.retrieve().bodyToMono(Movie.class).block();
*/
