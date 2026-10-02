package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminContinentDTO;
import com.duong.travelweb.model.dto.AdminCountryDTO;
import com.duong.travelweb.model.dto.AdminDestinationDTO;
import com.duong.travelweb.model.dto.AdminLandmarkDTO;
import com.duong.travelweb.model.dto.ContinentRequestDTO;
import com.duong.travelweb.model.dto.CountryRequestDTO;
import com.duong.travelweb.model.dto.DestinationRequestDTO;
import com.duong.travelweb.model.dto.LandmarkRequestDTO;
import com.duong.travelweb.model.entity.ContinentEntity;
import com.duong.travelweb.model.entity.CountryEntity;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.LandmarkEntity;
import com.duong.travelweb.repository.ContinentRepository;
import com.duong.travelweb.repository.CountryRepository;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.LandmarkRepository;
import com.duong.travelweb.service.AdminGeographyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminGeographyServiceImpl implements AdminGeographyService {
    private final ContinentRepository continentRepository;
    private final CountryRepository countryRepository;
    private final DestinationRepository destinationRepository;
    private final LandmarkRepository landmarkRepository;

    public AdminGeographyServiceImpl(ContinentRepository continentRepository,
                                     CountryRepository countryRepository,
                                     DestinationRepository destinationRepository,
                                     LandmarkRepository landmarkRepository) {
        this.continentRepository = continentRepository;
        this.countryRepository = countryRepository;
        this.destinationRepository = destinationRepository;
        this.landmarkRepository = landmarkRepository;
    }

    // ---- Continents ----

    @Override
    @Transactional(readOnly = true)
    public List<AdminContinentDTO> findContinents() {
        Map<UUID, Long> countryCounts = toCountMap(countryRepository.countGroupByContinent());
        return continentRepository.findAllOrdered().stream()
                .map(c -> toContinentDTO(c, countryCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public AdminContinentDTO createContinent(ContinentRequestDTO request) {
        ContinentEntity continent = new ContinentEntity();
        LocalDateTime now = LocalDateTime.now();
        continent.setCreatedAt(now);
        continent.setUpdatedAt(now);
        applyContinent(continent, request, null);
        return toContinentDTO(continentRepository.save(continent), 0);
    }

    @Override
    @Transactional
    public AdminContinentDTO updateContinent(UUID continentId, ContinentRequestDTO request) {
        ContinentEntity continent = findContinent(continentId);
        applyContinent(continent, request, continentId);
        continent.setUpdatedAt(LocalDateTime.now());
        continent = continentRepository.save(continent);
        return toContinentDTO(continent, toCountMap(countryRepository.countGroupByContinent()).getOrDefault(continentId, 0L));
    }

    @Override
    @Transactional
    public void deleteContinent(UUID continentId) {
        ContinentEntity continent = findContinent(continentId);
        long countries = toCountMap(countryRepository.countGroupByContinent()).getOrDefault(continentId, 0L);
        if (countries > 0) {
            throw ApiException.conflict("Châu lục còn " + countries + " quốc gia, không thể xoá");
        }
        continentRepository.delete(continent);
    }

    private void applyContinent(ContinentEntity continent, ContinentRequestDTO request, UUID excludeId) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        String name = request.getName().trim();
        if (continentRepository.existsByCode(code, excludeId)) {
            throw ApiException.conflict("Mã châu lục " + code + " đã tồn tại");
        }
        if (continentRepository.existsByName(name, excludeId)) {
            throw ApiException.conflict("Châu lục \"" + name + "\" đã tồn tại");
        }
        continent.setCode(code);
        continent.setName(name);
    }

    private ContinentEntity findContinent(UUID continentId) {
        return continentRepository.findById(continentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy châu lục"));
    }

    private AdminContinentDTO toContinentDTO(ContinentEntity entity, long countryCount) {
        AdminContinentDTO dto = new AdminContinentDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCountryCount(countryCount);
        return dto;
    }

    // ---- Countries ----

    @Override
    @Transactional(readOnly = true)
    public List<AdminCountryDTO> findCountries(String keyword, UUID continentId) {
        String kw = normalizeKeyword(keyword);
        Map<UUID, Long> destinationCounts = toCountMap(destinationRepository.countGroupByCountry());
        // Chỉ vài trăm quốc gia → lọc từ khoá trong bộ nhớ
        return countryRepository.findAllForAdmin(continentId).stream()
                .filter(c -> kw == null
                        || c.getName().toLowerCase(Locale.ROOT).contains(kw)
                        || c.getCountryCode().toLowerCase(Locale.ROOT).contains(kw))
                .map(c -> toCountryDTO(c, destinationCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public AdminCountryDTO createCountry(CountryRequestDTO request) {
        CountryEntity country = new CountryEntity();
        LocalDateTime now = LocalDateTime.now();
        country.setCreatedAt(now);
        country.setUpdatedAt(now);
        applyCountry(country, request, null);
        return toCountryDTO(countryRepository.save(country), 0);
    }

    @Override
    @Transactional
    public AdminCountryDTO updateCountry(UUID countryId, CountryRequestDTO request) {
        CountryEntity country = findCountry(countryId);
        applyCountry(country, request, countryId);
        country.setUpdatedAt(LocalDateTime.now());
        country = countryRepository.save(country);
        return toCountryDTO(country, toCountMap(destinationRepository.countGroupByCountry()).getOrDefault(countryId, 0L));
    }

    @Override
    @Transactional
    public void deleteCountry(UUID countryId) {
        CountryEntity country = findCountry(countryId);
        long destinations = toCountMap(destinationRepository.countGroupByCountry()).getOrDefault(countryId, 0L);
        if (destinations > 0) {
            throw ApiException.conflict("Quốc gia còn " + destinations + " điểm đến, không thể xoá");
        }
        countryRepository.delete(country);
    }

    private void applyCountry(CountryEntity country, CountryRequestDTO request, UUID excludeId) {
        String name = request.getName().trim();
        String code = request.getCountryCode().trim().toUpperCase(Locale.ROOT);
        String slug = request.getSlug() == null || request.getSlug().isBlank() ? slugify(name) : request.getSlug().trim();
        if (slug.isEmpty()) {
            throw ApiException.badRequest("Không tạo được slug từ tên, hãy nhập slug");
        }
        if (countryRepository.existsByCode(code, excludeId)) {
            throw ApiException.conflict("Mã quốc gia " + code + " đã tồn tại");
        }
        if (countryRepository.existsBySlug(slug, excludeId)) {
            throw ApiException.conflict("Slug \"" + slug + "\" đã tồn tại");
        }
        country.setContinent(findContinent(request.getContinentId()));
        country.setName(name);
        country.setCountryCode(code);
        country.setSlug(slug);
        country.setImageUrl(blankToNull(request.getImageUrl()));
        country.setDescription(blankToNull(request.getDescription()));
        country.setLatitude(request.getLatitude());
        country.setLongitude(request.getLongitude());
    }

    private CountryEntity findCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy quốc gia"));
    }

    private AdminCountryDTO toCountryDTO(CountryEntity entity, long destinationCount) {
        AdminCountryDTO dto = new AdminCountryDTO();
        dto.setId(entity.getId());
        if (entity.getContinent() != null) {
            dto.setContinentId(entity.getContinent().getId());
            dto.setContinentName(entity.getContinent().getName());
        }
        dto.setCountryCode(entity.getCountryCode());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setImageUrl(entity.getImageUrl());
        dto.setDescription(entity.getDescription());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setDestinationCount(destinationCount);
        return dto;
    }

    // ---- Destinations ----

    @Override
    @Transactional(readOnly = true)
    public List<AdminDestinationDTO> findDestinations(String keyword, UUID countryId, Boolean active, int page, int limit) {
        List<DestinationEntity> destinations = destinationRepository.findForAdmin(normalizeKeyword(keyword), countryId, active, page, limit);
        if (destinations.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = destinations.stream().map(DestinationEntity::getId).toList();
        Map<UUID, Long> hotelCounts = toCountMap(destinationRepository.countHotelsByDestinationIds(ids));
        Map<UUID, Long> landmarkCounts = toCountMap(destinationRepository.countLandmarksByDestinationIds(ids));
        return destinations.stream()
                .map(d -> toDestinationDTO(d, hotelCounts.getOrDefault(d.getId(), 0L), landmarkCounts.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDestinations(String keyword, UUID countryId, Boolean active) {
        return destinationRepository.countForAdmin(normalizeKeyword(keyword), countryId, active);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDestinationDTO getDestination(UUID destinationId) {
        return toDestinationDetail(findDestination(destinationId));
    }

    @Override
    @Transactional
    public AdminDestinationDTO createDestination(DestinationRequestDTO request) {
        DestinationEntity destination = new DestinationEntity();
        destination.setIsPopular(false);
        destination.setIsActive(true);
        destination.setCreatedAt(LocalDateTime.now());
        applyDestination(destination, request);
        return toDestinationDTO(destinationRepository.save(destination), 0, 0);
    }

    @Override
    @Transactional
    public AdminDestinationDTO updateDestination(UUID destinationId, DestinationRequestDTO request) {
        DestinationEntity destination = findDestination(destinationId);
        applyDestination(destination, request);
        return toDestinationDetail(destinationRepository.save(destination));
    }

    @Override
    @Transactional
    public void deactivateDestination(UUID destinationId) {
        DestinationEntity destination = findDestination(destinationId);
        if (Boolean.TRUE.equals(destination.getIsActive())) {
            destination.setIsActive(false);
            destinationRepository.save(destination);
        }
    }

    private void applyDestination(DestinationEntity destination, DestinationRequestDTO request) {
        destination.setCountry(findCountry(request.getCountryId()));
        destination.setName(request.getName().trim());
        destination.setDescription(blankToNull(request.getDescription()));
        destination.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        destination.setLatitude(request.getLatitude());
        destination.setLongitude(request.getLongitude());
        if (request.getIsPopular() != null) {
            destination.setIsPopular(request.getIsPopular());
        }
        if (request.getIsActive() != null) {
            destination.setIsActive(request.getIsActive());
        }
    }

    private DestinationEntity findDestination(UUID destinationId) {
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy điểm đến"));
    }

    private AdminDestinationDTO toDestinationDetail(DestinationEntity destination) {
        List<UUID> ids = List.of(destination.getId());
        long hotels = toCountMap(destinationRepository.countHotelsByDestinationIds(ids)).getOrDefault(destination.getId(), 0L);
        long landmarks = toCountMap(destinationRepository.countLandmarksByDestinationIds(ids)).getOrDefault(destination.getId(), 0L);
        return toDestinationDTO(destination, hotels, landmarks);
    }

    private AdminDestinationDTO toDestinationDTO(DestinationEntity entity, long hotelCount, long landmarkCount) {
        AdminDestinationDTO dto = new AdminDestinationDTO();
        dto.setId(entity.getId());
        if (entity.getCountry() != null) {
            dto.setCountryId(entity.getCountry().getId());
            dto.setCountryName(entity.getCountry().getName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setIsPopular(entity.getIsPopular());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setHotelCount(hotelCount);
        dto.setLandmarkCount(landmarkCount);
        return dto;
    }

    // ---- Landmarks ----

    @Override
    @Transactional(readOnly = true)
    public List<AdminLandmarkDTO> findLandmarks(String keyword, UUID destinationId, String category, Boolean active, int page, int limit) {
        return landmarkRepository.findForAdmin(normalizeKeyword(keyword), destinationId, blankToNull(category), active, page, limit)
                .stream().map(this::toLandmarkDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLandmarks(String keyword, UUID destinationId, String category, Boolean active) {
        return landmarkRepository.countForAdmin(normalizeKeyword(keyword), destinationId, blankToNull(category), active);
    }

    @Override
    @Transactional
    public AdminLandmarkDTO createLandmark(LandmarkRequestDTO request) {
        LandmarkEntity landmark = new LandmarkEntity();
        landmark.setCategory("other");
        landmark.setIsActive(true);
        LocalDateTime now = LocalDateTime.now();
        landmark.setCreatedAt(now);
        landmark.setUpdatedAt(now);
        applyLandmark(landmark, request);
        return toLandmarkDTO(landmarkRepository.save(landmark));
    }

    @Override
    @Transactional
    public AdminLandmarkDTO updateLandmark(UUID landmarkId, LandmarkRequestDTO request) {
        LandmarkEntity landmark = findLandmark(landmarkId);
        applyLandmark(landmark, request);
        landmark.setUpdatedAt(LocalDateTime.now());
        return toLandmarkDTO(landmarkRepository.save(landmark));
    }

    @Override
    @Transactional
    public void deactivateLandmark(UUID landmarkId) {
        LandmarkEntity landmark = findLandmark(landmarkId);
        if (Boolean.TRUE.equals(landmark.getIsActive())) {
            landmark.setIsActive(false);
            landmark.setUpdatedAt(LocalDateTime.now());
            landmarkRepository.save(landmark);
        }
    }

    private void applyLandmark(LandmarkEntity landmark, LandmarkRequestDTO request) {
        landmark.setDestination(findDestination(request.getDestinationId()));
        landmark.setName(request.getName().trim());
        landmark.setDescription(blankToNull(request.getDescription()));
        landmark.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        landmark.setOpeningHours(blankToNull(request.getOpeningHours()));
        landmark.setEntryFee(request.getEntryFee());
        landmark.setAddress(blankToNull(request.getAddress()));
        landmark.setPhone(blankToNull(request.getPhone()));
        landmark.setWebsite(blankToNull(request.getWebsite()));
        landmark.setLatitude(request.getLatitude());
        landmark.setLongitude(request.getLongitude());
        if (request.getCategory() != null) {
            landmark.setCategory(request.getCategory());
        }
        if (request.getIsActive() != null) {
            landmark.setIsActive(request.getIsActive());
        }
    }

    private LandmarkEntity findLandmark(UUID landmarkId) {
        return landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy địa danh"));
    }

    private AdminLandmarkDTO toLandmarkDTO(LandmarkEntity entity) {
        AdminLandmarkDTO dto = new AdminLandmarkDTO();
        dto.setId(entity.getId());
        if (entity.getDestination() != null) {
            dto.setDestinationId(entity.getDestination().getId());
            dto.setDestinationName(entity.getDestination().getName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setOpeningHours(entity.getOpeningHours());
        dto.setEntryFee(entity.getEntryFee());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setWebsite(entity.getWebsite());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // ---- Helpers ----

    /** "Việt Nam" → "viet-nam" */
    static String slugify(String input) {
        String normalized = Normalizer.normalize(input.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
