ANT = ant
BUILD_FILE = WVA_App/build.xml
REV_TASK = rev -Drev=
REV = $(shell sh get_rev.sh)

release:
	@echo "Building rev $(REV) ..."
	@$(ANT) -f $(BUILD_FILE) $(REV_TASK)$(REV)
